package com.smartfarm.user;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smartfarm.ApiResponse;

import jakarta.validation.Valid;

@RestController
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
@RequestMapping("/users")
public class UserController {

	private final UserService service;

	public UserController(UserService service) {
		this.service = service;
	}

	@GetMapping("/check-bootstrap")
	public ResponseEntity<ApiResponse<BootstrapStatusResponse>> checkBootstrap() {
		return service.checkBootstrap();
	}

	@PostMapping("/signup")
	public ResponseEntity<ApiResponse<?>> addUser(@Valid @RequestBody SignupRequest request) {	
		return service.signUp(request);
	}
	
	@PostMapping("/login")
	public ResponseEntity<ApiResponse<?>> login(@Valid @RequestBody LoginRequest request) {	
		return service.login(request); 
	}

	@PostMapping("/forgot-password")
	public ResponseEntity<ApiResponse<?>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
		return service.forgotPassword(request);
	}

	@PostMapping("/reset-password")
	public ResponseEntity<ApiResponse<?>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
		return service.resetPassword(request);
	}

	@PatchMapping("/{id}/admin-reset-password")
	public ResponseEntity<ApiResponse<User>> adminResetPassword(
			@PathVariable String id,
			@Valid @RequestBody AdminResetPasswordRequest request) {
		return service.adminResetPassword(id, request);
	}

	@GetMapping
	public ResponseEntity<ApiResponse<List<User>>> getAllUsers(
			@RequestParam(required = false) String role,
			@RequestParam(required = false) String createdById,
			@RequestParam(required = false) String managerId) {
		return service.getAllUsers(role, createdById, managerId);
	}

	@GetMapping("/{id}")
	public ResponseEntity<ApiResponse<User>> getUserById(@PathVariable String id) {
		return service.getUserById(id);
	}

	@PostMapping("/create")
	public ResponseEntity<ApiResponse<User>> createStaff(@Valid @RequestBody CreateStaffRequest request) {
		return service.createStaff(request);
	}

	@PatchMapping("/{id}/status")
	public ResponseEntity<ApiResponse<User>> updateUserStatus(
			@PathVariable String id,
			@Valid @RequestBody UpdateUserStatusRequest request) {
		return service.updateUserStatus(id, request);
	}

	@PatchMapping("/{id}/privileges")
	public ResponseEntity<ApiResponse<User>> updatePrivileges(
			@PathVariable String id,
			@RequestBody UpdatePrivilegesRequest request) {
		return service.updatePrivileges(id, request);
	}

	@PutMapping("/{id}/categories")
	public ResponseEntity<ApiResponse<User>> assignCategories(
			@PathVariable String id,
			@RequestBody AssignCategoriesRequest request) {
		return service.assignCategories(id, request);
	}

	@PutMapping("/{id}/projects")
	public ResponseEntity<ApiResponse<Void>> assignProjects(
			@PathVariable String id,
			@RequestBody AssignProjectsRequest request) {
		return service.assignProjectsToSupervisor(id, request);
	}

	@GetMapping("/{id}/projects")
	public ResponseEntity<ApiResponse<List<com.smartfarm.projects.Project>>> getSupervisorProjects(
			@PathVariable String id) {
		return service.getSupervisorProjects(id);
	}

	@PutMapping("/{id}/profile")
	public ResponseEntity<ApiResponse<User>> updateProfile(
			@PathVariable String id,
			@RequestBody UpdateProfileRequest request) {
		return service.updateProfile(id, request);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable String id) {
		return service.deleteUser(id);
	}
}