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

import jakarta.persistence.EntityNotFoundException;

@Service
public class UserService {

	private final UserRepository userRepo;
	private final CategoryRepository categoryRepo;
	private final ProjectRepository projectRepo;
	private final PasswordEncoder passwordEncoder;

	private static final long MAX_ADMINS = 1;
	private static final long MAX_MANAGERS = 2;
	private static final long MAX_SUPERVISORS = 10;

	public UserService(UserRepository userRepo, CategoryRepository categoryRepo, ProjectRepository projectRepo, PasswordEncoder passwordEncoder) {
		this.userRepo = userRepo;
		this.categoryRepo = categoryRepo;
		this.projectRepo = projectRepo;
		this.passwordEncoder = passwordEncoder;
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
		return ResponseEntity.ok(new ApiResponse<>(status, "System status retrieved", true, Instant.now()));
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
				return ResponseEntity.status(400).body(new ApiResponse<>(null, "Invalid role selected.", false, Instant.now()));
			}

			assignedStatus = "PENDING_APPROVAL";
			successMessage = "Account created! Your registration is pending Administrator approval before you can sign in.";
		}

		long count = userRepo.count();
		String id = IdGenarator.generateId(username, count);
		while (userRepo.existsById(id)) {
			count++;
			id = IdGenarator.generateId(username, count);
		}

		String hashedPassword = passwordEncoder.encode(password);
		User user = new User(id, username, hashedPassword, assignedRole, assignedStatus, null);
		User saved = userRepo.save(user);

		return ResponseEntity.status(201).body(new ApiResponse<>(saved, successMessage, true, Instant.now()));
	}

	public ResponseEntity<ApiResponse<?>> login(LoginRequest request) {
		String username = request.username().trim();
		String password = request.password().trim();

		User user = userRepo.findByUsername(username);
		if (user == null) {
			return ResponseEntity.status(400).body(new ApiResponse<>(null, "Invalid username or password.", false, Instant.now()));
		}

		boolean passwordMatches = passwordEncoder.matches(password, user.getPassword());
		if (!passwordMatches) {
			return ResponseEntity.status(400).body(new ApiResponse<>(null, "Invalid username or password.", false, Instant.now()));
		}

		if ("PENDING_APPROVAL".equalsIgnoreCase(user.getStatus())) {
			return ResponseEntity.status(403).body(new ApiResponse<>(null, "Account pending approval. Please contact your farm Administrator to activate your account.", false, Instant.now()));
		}

		if ("DISABLED".equalsIgnoreCase(user.getStatus())) {
			return ResponseEntity.status(403).body(new ApiResponse<>(null, "Account deactivated. Please contact your farm Administrator.", false, Instant.now()));
		}

		return ResponseEntity.status(200).body(new ApiResponse<>(user, "Login successful", true, Instant.now()));
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

		String hashedPassword = passwordEncoder.encode(password);
		User user = new User(id, username, hashedPassword, role, "ACTIVE", request.createdById());
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
	public ResponseEntity<ApiResponse<Void>> deleteUser(String id) {
		User user = userRepo.findById(id)
			.orElseThrow(() -> new EntityNotFoundException("User not found: " + id));

		if ("ADMIN".equalsIgnoreCase(user.getRole())) {
			return ResponseEntity.status(400).body(new ApiResponse<>(null, "Cannot delete the primary Farm Administrator.", false, Instant.now()));
		}

		userRepo.delete(user);
		return ResponseEntity.ok(new ApiResponse<>(null, "User removed successfully", true, Instant.now()));
	}
}
