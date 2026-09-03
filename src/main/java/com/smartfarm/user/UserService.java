package com.smartfarm.user;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartfarm.ApiResponse;
import com.smartfarm.category.Category;
import com.smartfarm.category.CategoryRepository;
import com.smartfarm.util.IdGenarator;

import com.smartfarm.projects.Project;
import com.smartfarm.projects.ProjectRepository;
import com.smartfarm.auth.PasswordResetToken;
import com.smartfarm.auth.PasswordResetTokenRepository;
import com.smartfarm.auth.EmailService;
import java.util.UUID;
import java.time.temporal.ChronoUnit;

import jakarta.persistence.EntityNotFoundException;

@Service
public class UserService {

	private final UserRepository userRepo;
	private final CategoryRepository categoryRepo;
	private final ProjectRepository projectRepo;
	private final PasswordEncoder passwordEncoder;
	private final PasswordResetTokenRepository tokenRepo;
	private final EmailService emailService;

	private static final long MAX_MANAGERS = 2; 
	private static final long MAX_SUPERVISORS = 10;

	public UserService(UserRepository userRepo, CategoryRepository categoryRepo, ProjectRepository projectRepo, PasswordEncoder passwordEncoder, PasswordResetTokenRepository tokenRepo, EmailService emailService) {
		this.userRepo = userRepo;
		this.categoryRepo = categoryRepo;
		this.projectRepo = projectRepo;
		this.passwordEncoder = passwordEncoder;
		this.tokenRepo = tokenRepo;
		this.emailService = emailService;
	}

	public ResponseEntity<ApiResponse<BootstrapStatusResponse>> checkBootstrap() {
		long total = userRepo.count();
		long admins = userRepo.countByRoleIgnoreCase("ADMIN");
		long managers = userRepo.countByRoleIgnoreCase("MANAGER");
		long supervisors = userRepo.countByRoleIgnoreCase("SUPERVISOR");

		BootstrapStatusResponse status = new BootstrapStatusResponse(
			total == 0,
			total,
			admins,
			managers,
			supervisors,
			MAX_MANAGERS,
			MAX_SUPERVISORS
		);
		return ResponseEntity.ok(new ApiResponse<>(status, "System status retrieved ✅", true, Instant.now()));
	}

	@Transactional
	public ResponseEntity<ApiResponse<?>> signUp(SignupRequest request) {
		String username = request.username().trim();
		String password = request.password().trim();
		String cpassword = request.cpassword().trim();

		if (!password.equals(cpassword)) {
			return ResponseEntity.status(400).body(new ApiResponse<>(null, "Password mismatch!", false, Instant.now()));
		}
		if (userRepo.existsByUsername(username)) {
			return ResponseEntity.status(400).body(new ApiResponse<>(null, "Username already exists! Try a different username.", false, Instant.now()));
		}

		long totalUsers = userRepo.count();
		long adminCount = userRepo.countByRoleIgnoreCase("ADMIN");

		String assignedRole;
		String assignedStatus;
		String successMessage;

		if (totalUsers == 0 || adminCount == 0) {
			// First user in the system is automatically the Administrator
			assignedRole = "ADMIN";
			assignedStatus = "ACTIVE";
			successMessage = "Welcome Administrator! Your primary farm administrator account has been created ✅";
		} else {
			// Non-bootstrap signup: request as Manager or Supervisor, pending approval
			String requestedRole = (request.role() != null && !request.role().trim().isEmpty()) 
				? request.role().trim().toUpperCase() 
				: "MANAGER";

			if ("ADMIN".equalsIgnoreCase(requestedRole)) {
				return ResponseEntity.status(400).body(new ApiResponse<>(null, "Only 1 Administrator account is permitted on the farm portal.", false, Instant.now()));
			}

			if ("MANAGER".equalsIgnoreCase(requestedRole)) {
				long managerCount = userRepo.countByRoleIgnoreCase("MANAGER");
				if (managerCount >= MAX_MANAGERS) {
					return ResponseEntity.status(400).body(new ApiResponse<>(null, "Manager quota reached (Maximum of " + MAX_MANAGERS + " managers).", false, Instant.now()));
				}
				assignedRole = "MANAGER";
			} else if ("SUPERVISOR".equalsIgnoreCase(requestedRole)) {
				long supervisorCount = userRepo.countByRoleIgnoreCase("SUPERVISOR");
				if (supervisorCount >= MAX_SUPERVISORS) {
					return ResponseEntity.status(400).body(new ApiResponse<>(null, "Supervisor quota reached (Maximum of " + MAX_SUPERVISORS + " supervisors).", false, Instant.now()));
				}
				assignedRole = "SUPERVISOR";
			} else {
				return ResponseEntity.status(400).body(new ApiResponse<>(null, "Invalid role selected!", false, Instant.now()));
			}

			assignedStatus = "PENDING_APPROVAL";
			successMessage = "Account created! Your registration is pending Administrator approval before you can sign in. Please contact your admin to confirm.";
		}

		long count = userRepo.count();
		String id = IdGenarator.generateId(username, count);
		while (userRepo.existsById(id)) {
			count++;
			id = IdGenarator.generateId(username, count);
		}

		String email = (request.email() != null && !request.email().trim().isEmpty()) ? request.email().trim() : null;
		if (email != null && userRepo.findByEmail(email).isPresent()) {
			return ResponseEntity.status(400).body(new ApiResponse<>(null, "Email already registered! Try a different email.", false, Instant.now()));
		}

		String hashedPassword = passwordEncoder.encode(password);
		User user = new User(id, username, email, hashedPassword, assignedRole, assignedStatus, null);
		User saved = userRepo.save(user);

		return ResponseEntity.status(201).body(new ApiResponse<>(saved, successMessage, true, Instant.now()));
	}

	public ResponseEntity<ApiResponse<?>> login(LoginRequest request) {
		String identifier = request.username().trim();
		String password = request.password().trim();

		User user = userRepo.findOptionalByUsername(identifier)
				.orElseGet(() -> userRepo.findByEmail(identifier).orElse(null));

		if (user == null) {
			return ResponseEntity.status(400).body(new ApiResponse<>(null, "Invalid username/email or password.", false, Instant.now()));
		}

		boolean passwordMatches = passwordEncoder.matches(password, user.getPassword());
		if (!passwordMatches) {
			return ResponseEntity.status(400).body(new ApiResponse<>(null, "Invalid username/email or password.", false, Instant.now()));
		}

		if ("PENDING_APPROVAL".equalsIgnoreCase(user.getStatus())) {
			return ResponseEntity.status(403).body(new ApiResponse<>(null, "Account pending approval. Please contact your farm Administrator to activate your account.", false, Instant.now()));
		}

		if ("DISABLED".equalsIgnoreCase(user.getStatus())) {
			return ResponseEntity.status(403).body(new ApiResponse<>(null, "Account deactivated. Please contact your farm Administrator.", false, Instant.now()));
		}

		return ResponseEntity.status(200).body(new ApiResponse<>(user, "Login successful", true, Instant.now()));
	}

	public ResponseEntity<ApiResponse<?>> forgotPassword(ForgotPasswordRequest request) {
		String email = request.email().trim();
		User user = userRepo.findByEmail(email).orElse(null);
		
		if (user == null) {
			return ResponseEntity.status(400).body(new ApiResponse<>(null, "No account found with this email address. If you did not register an email, please contact your Farm Administrator to reset your password.", false, Instant.now()));
		}

		// Delete existing token if it exists
		tokenRepo.findByUserId(user.getId()).ifPresent(tokenRepo::delete);

		String tokenString = UUID.randomUUID().toString();
		PasswordResetToken token = new PasswordResetToken(
			tokenString,
			user,
			Instant.now().plus(15, ChronoUnit.MINUTES) // valid for 15 mins
		);
		tokenRepo.save(token);

		// Use FRONTEND_URL environment variable if present, otherwise default to live Vercel production URL
		String frontendUrl = System.getenv("FRONTEND_URL") != null 
			? System.getenv("FRONTEND_URL").replaceAll("/+$", "") 
			: "https://smartfarm-frontend-jade.vercel.app";
		String resetLink = frontendUrl + "/reset-password?token=" + tokenString;
		
		try {
			emailService.sendPasswordResetEmail(email, resetLink);
		} catch (Exception e) {
			return ResponseEntity.status(500).body(new ApiResponse<>(null, "Failed to send email. Check mail configuration.", false, Instant.now()));
		}

		return ResponseEntity.ok(new ApiResponse<>(null, "Password reset link has been sent to your email. Please check your inbox.", true, Instant.now()));
	}

	@Transactional
	public ResponseEntity<ApiResponse<?>> resetPassword(ResetPasswordRequest request) {
		String tokenString = request.token().trim();
		String newPassword = request.newPassword().trim();

		PasswordResetToken token = tokenRepo.findById(tokenString).orElse(null);
		
		if (token == null) {
			return ResponseEntity.status(400).body(new ApiResponse<>(null, "Invalid or expired token.", false, Instant.now()));
		}

		if (token.getExpiryDate().isBefore(Instant.now())) {
			tokenRepo.delete(token);
			return ResponseEntity.status(400).body(new ApiResponse<>(null, "Token has expired. Please request a new one.", false, Instant.now()));
		}

		User user = token.getUser();
		user.setPassword(passwordEncoder.encode(newPassword));
		userRepo.save(user);
		
		tokenRepo.delete(token); // Delete token after use

		return ResponseEntity.ok(new ApiResponse<>(null, "Password successfully reset. You can now log in.", true, Instant.now()));
	}

	@Transactional
	public ResponseEntity<ApiResponse<User>> adminResetPassword(String userId, AdminResetPasswordRequest request) {
		User user = userRepo.findById(userId)
				.orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));

		user.setPassword(passwordEncoder.encode(request.newPassword().trim()));
		userRepo.save(user);

		return ResponseEntity.ok(new ApiResponse<>(user, "Password has been successfully updated by Admin.", true, Instant.now()));
	}

	public ResponseEntity<ApiResponse<List<User>>> getAllUsers(String role, String createdById) {
		List<User> users;
		if (createdById != null && !createdById.trim().isEmpty()) {
			users = userRepo.findByCreatedById(createdById.trim());
		} else if (role != null && !role.trim().isEmpty()) {
			users = userRepo.findByRoleIgnoreCase(role.trim());
		} else {
			users = userRepo.findAll();
		}
		return ResponseEntity.ok(new ApiResponse<>(users, "Users fetched successfully", true, Instant.now()));
	}

	public ResponseEntity<ApiResponse<User>> getUserById(String id) {
		User user = userRepo.findById(id)
			.orElseThrow(() -> new EntityNotFoundException("User not found: " + id));
		return ResponseEntity.ok(new ApiResponse<>(user, "User details fetched successfully", true, Instant.now()));
	}

	@Transactional
	public ResponseEntity<ApiResponse<User>> createStaff(CreateStaffRequest request) {
		String username = request.username().trim();
		String password = request.password().trim();
		String role = request.role().trim().toUpperCase();

		if (userRepo.existsByUsername(username)) {
			return ResponseEntity.status(400).body(new ApiResponse<>(null, "Username already in use.", false, Instant.now()));
		}

		if ("MANAGER".equalsIgnoreCase(role)) {
			long managerCount = userRepo.countByRoleIgnoreCase("MANAGER");
			if (managerCount >= MAX_MANAGERS) {
				return ResponseEntity.status(400).body(new ApiResponse<>(null, "Manager quota reached (Max " + MAX_MANAGERS + ").", false, Instant.now()));
			}
		} else if ("SUPERVISOR".equalsIgnoreCase(role)) {
			long supervisorCount = userRepo.countByRoleIgnoreCase("SUPERVISOR");
			if (supervisorCount >= MAX_SUPERVISORS) {
				return ResponseEntity.status(400).body(new ApiResponse<>(null, "Supervisor quota reached (Max " + MAX_SUPERVISORS + ").", false, Instant.now()));
			}
		} else {
			return ResponseEntity.status(400).body(new ApiResponse<>(null, "Invalid role. Only MANAGER or SUPERVISOR can be added.", false, Instant.now()));
		}

		long count = userRepo.count();
		String id = IdGenarator.generateId(username, count);
		while (userRepo.existsById(id)) {
			count++;
			id = IdGenarator.generateId(username, count);
		}

		String email = (request.email() != null && !request.email().trim().isEmpty()) ? request.email().trim() : null;
		if (email != null && userRepo.findByEmail(email).isPresent()) {
			return ResponseEntity.status(400).body(new ApiResponse<>(null, "Email already registered! Try a different email.", false, Instant.now()));
		}

		String hashedPassword = passwordEncoder.encode(password);
		User user = new User(id, username, email, hashedPassword, role, "ACTIVE", request.createdById());
		User saved = userRepo.save(user);

		return ResponseEntity.status(201).body(new ApiResponse<>(saved, role + " created and activated successfully.", true, Instant.now()));
	}

	@Transactional
	public ResponseEntity<ApiResponse<User>> updateUserStatus(String id, UpdateUserStatusRequest request) {
		User user = userRepo.findById(id)
			.orElseThrow(() -> new EntityNotFoundException("User not found: " + id));

		String newStatus = request.status().trim().toUpperCase();
		user.setStatus(newStatus);
		User saved = userRepo.save(user);

		return ResponseEntity.ok(new ApiResponse<>(saved, "User status updated to " + newStatus, true, Instant.now()));
	}

	@Transactional
	public ResponseEntity<ApiResponse<User>> assignCategories(String id, AssignCategoriesRequest request) {
		User user = userRepo.findById(id)
			.orElseThrow(() -> new EntityNotFoundException("User not found: " + id));

		Set<Category> categories = new HashSet<>();
		if (request.categoryIds() != null) {
			for (String catId : request.categoryIds()) {
				categoryRepo.findById(catId).ifPresent(categories::add);
			}
		}

		user.setAssignedCategories(categories);
		User saved = userRepo.save(user);

		return ResponseEntity.ok(new ApiResponse<>(saved, "Categories assigned to " + user.getUsername(), true, Instant.now()));
	}

	@Transactional
	public ResponseEntity<ApiResponse<Void>> assignProjectsToSupervisor(String supervisorId, AssignProjectsRequest request) {
		User supervisor = userRepo.findById(supervisorId)
			.orElseThrow(() -> new EntityNotFoundException("Supervisor not found: " + supervisorId));

		// Unassign all existing projects for this supervisor
		List<Project> currentlyAssigned = projectRepo.findBySupervisorId(supervisorId);
		for (Project p : currentlyAssigned) {
			p.setSupervisor(null);
			projectRepo.save(p);
		}

		// Assign selected projects
		if (request.projectIds() != null) {
			for (String projId : request.projectIds()) {
				projectRepo.findById(projId).ifPresent(p -> {
					p.setSupervisor(supervisor);
					projectRepo.save(p);
				});
			}
		}

		return ResponseEntity.ok(new ApiResponse<>(null, "Projects assigned to " + supervisor.getUsername() + " successfully.", true, Instant.now()));
	}

	public ResponseEntity<ApiResponse<List<Project>>> getSupervisorProjects(String supervisorId) {
		List<Project> projects = projectRepo.findBySupervisorId(supervisorId);
		return ResponseEntity.ok(new ApiResponse<>(projects, null, true, Instant.now()));
	}

	@Transactional
	public ResponseEntity<ApiResponse<User>> updateProfile(String id, UpdateProfileRequest request) {
		User user = userRepo.findById(id)
			.orElseThrow(() -> new EntityNotFoundException("User not found: " + id));

		if (request.username() != null && !request.username().trim().isEmpty()) {
			String newUsername = request.username().trim();
			if (!newUsername.equalsIgnoreCase(user.getUsername()) && userRepo.existsByUsername(newUsername)) {
				return ResponseEntity.status(400).body(new ApiResponse<>(null, "Username is already taken.", false, Instant.now()));
			}
			user.setUsername(newUsername);
		} 

		if (request.newPassword() != null && !request.newPassword().trim().isEmpty()) {
			if (request.currentPassword() != null && !request.currentPassword().trim().isEmpty()) {
				if (!passwordEncoder.matches(request.currentPassword().trim(), user.getPassword())) {
					return ResponseEntity.status(400).body(new ApiResponse<>(null, "Incorrect current password.", false, Instant.now()));
				}
			}
			user.setPassword(passwordEncoder.encode(request.newPassword().trim()));
		}

		User saved = userRepo.save(user);
		return ResponseEntity.ok(new ApiResponse<>(saved, "Profile credentials updated successfully", true, Instant.now()));
	}

	@Transactional
	public ResponseEntity<ApiResponse<Void>> deleteUser(String id) {
		User user = userRepo.findById(id)
			.orElseThrow(() -> new EntityNotFoundException("User not found: " + id));

		if ("ADMIN".equalsIgnoreCase(user.getRole())) {
			return ResponseEntity.status(400).body(new ApiResponse<>(null, "Cannot delete the primary Administrator account!", false, Instant.now()));
		}

		// 1. Delete associated password reset token if exists
		tokenRepo.findByUserId(id).ifPresent(tokenRepo::delete);

		// 2. Clear assigned categories (join table)
		if (user.getAssignedCategories() != null) {
			user.getAssignedCategories().clear();
			userRepo.saveAndFlush(user);
		}

		// 3. Unassign from supervised projects
		List<Project> supervisedProjects = projectRepo.findBySupervisorId(id);
		for (Project p : supervisedProjects) {
			p.setSupervisor(null);
			projectRepo.save(p);
		}

		// 4. Clear createdById reference for any users created by this user
		List<User> createdUsers = userRepo.findByCreatedById(id);
		for (User u : createdUsers) {
			u.setCreatedById(null);
			userRepo.save(u);
		}

		// 5. Delete the user
		userRepo.delete(user);
		return ResponseEntity.ok(new ApiResponse<>(null, "Account removed successfully", true, Instant.now()));
	}
}
