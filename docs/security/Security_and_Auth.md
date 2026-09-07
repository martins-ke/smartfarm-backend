# SmartFarm Security & Authentication (JWT Migration)

**Status:** In Progress
**Scope:** Backend Authentication & Route Protection

## 1. Current State (Header-Based Authentication)

Currently, the system relies on client-side state combined with raw headers to identify users. 
When a user logs in, the `User` object is saved to `localStorage` on the frontend, and the frontend manually attaches headers to subsequent API calls:

```http
GET /users
X-User-Id: 12345
X-User-Role: MANAGER
```

### 1.1 Vulnerabilities
*   **Spoofing:** A malicious user could use Postman or cURL to bypass the frontend entirely, injecting `X-User-Role: ADMIN` into the headers to gain full access to the system.
*   **Disabled Spring Security:** `SecurityConfig.java` currently has `.requestMatchers("/**").permitAll()`, meaning no endpoints are inherently protected at the HTTP level.
*   **Hardcoded Secrets:** `application.properties` contains plain-text credentials for the production database.

---

## 2. Planned Implementation (JWT Authentication)

To reach production readiness, we will migrate to a stateless JWT (JSON Web Token) architecture.

### 2.1 The JWT Flow
1. **Login:** User submits `username` and `password` to `POST /users/login`.
2. **Token Generation:** The backend verifies credentials and generates a signed JWT containing the user's `id`, `role`, and `privileges` as claims.
3. **Token Transmission:** The JWT is sent back to the frontend in the login response.
4. **Subsequent Requests:** The frontend attaches the token as a Bearer Token:
   `Authorization: Bearer <jwt_token>`
5. **Validation:** A custom Spring Security `OncePerRequestFilter` intercepts incoming requests, extracts the JWT, validates the signature, and populates the `SecurityContextHolder`.

### 2.2 Endpoint Protection (Spring Security)
Instead of manually checking `X-User-Role` in every controller, we will use Spring Security annotations and matchers:
*   `@PreAuthorize("hasAuthority('ADMIN')")` for strict admin endpoints.
*   `@PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")` for management endpoints.

### 2.3 Environmental Security
*   **Database Credentials:** Move `spring.datasource.password` out of the codebase and into system environment variables (e.g., `DB_PASSWORD`).
*   **JPA Auto-DDL:** Change `spring.jpa.hibernate.ddl-auto=update` to `validate` or `none` in production to prevent accidental schema overwrites.
*   **JWT Secret Key:** Ensure the signing key is robust (HMAC-SHA256) and injected via environment variables.

---

## 3. Migration Steps (SDLC Implementation Phase)

1. **Dependency Addition:** Add `io.jsonwebtoken` (jjwt) dependencies to `pom.xml`.
2. **JwtService Creation:** Build a utility class for generating, signing, and validating tokens.
3. **Filter Implementation:** Create `JwtAuthenticationFilter` to intercept requests.
4. **Security Configuration:** Update `SecurityConfig.java` to enforce authenticated routes while whitelisting `/users/login` and `/users/signup`.
5. **Controller Refactoring:** Remove `X-User-Id` header dependencies from all Controllers and use `AuthenticationPrincipal`.
6. **Frontend Updates:** Modify `request.js` (apiClient) to append `Authorization: Bearer ...` instead of `X-User-Role`.
