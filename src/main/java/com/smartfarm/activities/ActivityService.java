package com.smartfarm.activities;

import java.time.Instant;
import java.time.LocalDate;

import org.springframework.http.ResponseEntity;
import com.smartfarm.user.User;
import org.springframework.stereotype.Service;

import com.smartfarm.ApiResponse;
import com.smartfarm.projects.Project;
import com.smartfarm.projects.ProjectRepository;
import com.smartfarm.util.IdGenarator;

import jakarta.persistence.EntityNotFoundException;

@Service
public class ActivityService {

	private final ActivityRepository activityRepo;
	private final ProjectRepository projectRepo;
	private final com.smartfarm.employees.EmployeeRepository employeeRepo;
	private final ActivityLaborAssignmentRepository laborRepo;
	private final com.smartfarm.expenses.ExpenseRepository expenseRepo;
	
	public ActivityService(ActivityRepository activityRepo, ProjectRepository projectRepo, com.smartfarm.user.UserRepository userRepo,
			com.smartfarm.employees.EmployeeRepository employeeRepo, ActivityLaborAssignmentRepository laborRepo,
			com.smartfarm.expenses.ExpenseRepository expenseRepo) {
		this.activityRepo = activityRepo;
		this.projectRepo = projectRepo;
		this.employeeRepo = employeeRepo;
		this.laborRepo = laborRepo;
		this.expenseRepo = expenseRepo;
	}

	public ResponseEntity<ApiResponse<java.util.List<ActivityLaborAssignment>>> getLaborAssignments(String activityId) {
		java.util.List<ActivityLaborAssignment> list = laborRepo.findByActivityId(activityId);
		return ResponseEntity.ok(new ApiResponse<>(list, "Labor assignments retrieved ✅", true, Instant.now()));
	}

	@org.springframework.transaction.annotation.Transactional
	public ResponseEntity<ApiResponse<?>> assignLaborToActivity(String activityId, AssignLaborRequest req) {
		Activity activity = activityRepo.findById(activityId)
				.orElseThrow(() -> new EntityNotFoundException("Activity not found with ID: " + activityId));

		com.smartfarm.employees.Employee employee = employeeRepo.findById(req.employeeId())
				.orElseThrow(() -> new EntityNotFoundException("Employee not found with ID: " + req.employeeId()));

		if (!"ACTIVE".equalsIgnoreCase(employee.getStatus())) {
			return ResponseEntity.status(400).body(new ApiResponse<>(null, "Cannot assign inactive employee to tasks!", false, Instant.now()));
		}

		java.math.BigDecimal daily = employee.getDailyRate() != null ? employee.getDailyRate() : java.math.BigDecimal.ZERO;
		double hours;
		java.math.BigDecimal wage;
		java.math.BigDecimal unitPrice;
		java.math.BigDecimal qty;

		if (req.daysWorked() != null && req.daysWorked() > 0) {
			double days = req.daysWorked();
			hours = days * 8.0;
			unitPrice = daily;
			qty = java.math.BigDecimal.valueOf(days);
			wage = daily.multiply(qty).setScale(2, java.math.RoundingMode.HALF_UP);
		} else {
			hours = req.hoursWorked() > 0 ? req.hoursWorked() : 8.0;
			java.math.BigDecimal hourly = daily.divide(java.math.BigDecimal.valueOf(8), 2, java.math.RoundingMode.HALF_UP);
			unitPrice = hourly;
			qty = java.math.BigDecimal.valueOf(hours);
			wage = hourly.multiply(qty).setScale(2, java.math.RoundingMode.HALF_UP);
		}

		LocalDate allocDate = req.assignmentDate() != null ? req.assignmentDate() : LocalDate.now();

		ActivityLaborAssignment assignment = new ActivityLaborAssignment(
			activity,
			employee,
			allocDate,
			hours,
			wage,
			req.notes()
		);

		ActivityLaborAssignment saved = laborRepo.save(assignment);

		// Record directly as Project Expense without creating any additional relational entity
		Project project = activity.getProject();
		if (project != null) {
			long count = expenseRepo.count();
			String expBase = "Labor: " + employee.getFullName();
			String expenseId = IdGenarator.generateId(expBase, count);
			while (expenseRepo.existsById(expenseId)) {
				count++;
				expenseId = IdGenarator.generateId(expBase, count);
			}

			String fullTitle = "Labor: " + employee.getFullName() + (activity.getTitle() != null && !activity.getTitle().isBlank() ? " (" + activity.getTitle() + ")" : "");
			String rateDesc = (req.daysWorked() != null && req.daysWorked() > 0)
					? (req.daysWorked() + " day(s) @ Ksh " + daily + "/day")
					: (hours + " hr(s) @ Ksh " + unitPrice + "/hr (Daily: Ksh " + daily + ")");
			String expNotes = "Worker: " + employee.getFullName() + " [" + employee.getEmploymentType() + "]. "
					+ rateDesc + ". "
					+ (req.notes() != null && !req.notes().isBlank() ? req.notes() : "");

			com.smartfarm.expenses.Expense expense = new com.smartfarm.expenses.Expense(
				expenseId,
				fullTitle,
				wage,
				unitPrice,
				qty,
				allocDate,
				expNotes,
				project
			);
			expenseRepo.save(expense);
		}

		return ResponseEntity.status(201).body(new ApiResponse<>(saved, "Labor assigned & wage (Ksh " + wage + ") added to project expenses ✅", true, Instant.now()));
	}
	
	public ResponseEntity<ApiResponse<Activity>> recordActivity(CreateActivityRequest request, User currentUser){
		Project project = projectRepo.findById(request.project_id()).orElseThrow(()-> new EntityNotFoundException("Project not in the system!"));

		if ("SUPERVISOR".equalsIgnoreCase(currentUser.getRole())) {
			boolean isAssigned = project.getSupervisor() != null && currentUser != null && currentUser.getId().trim().equals(project.getSupervisor().getId());
			if (!isAssigned) {
				return ResponseEntity.status(403).body(new ApiResponse<>(null, "Access Denied: You are not assigned to supervise this project.", false, Instant.now()));
			}
			if (currentUser != null) {
				com.smartfarm.user.User sup = currentUser;
				if (sup == null || sup.getPrivileges() == null || !sup.getPrivileges().contains("CAN_LOG_ACTIVITIES")) {
					return ResponseEntity.status(403).body(new ApiResponse<>(null, "Access Denied: You do not have privilege to log daily field activities.", false, Instant.now()));
				}
			}
		} else if ("MANAGER".equalsIgnoreCase(currentUser.getRole()) && currentUser != null && !currentUser.getId().trim().isEmpty()) {
			com.smartfarm.user.User manager = currentUser;
			if (manager != null) {
				boolean isAssigned = manager.getAssignedCategories().stream()
						.anyMatch(c -> c.getId().equalsIgnoreCase(project.getCategory().getId()));
				if (!isAssigned) {
					return ResponseEntity.status(403).body(new ApiResponse<>(null, "Access Denied: You are not assigned to manage the category for this project.", false, Instant.now()));
				}
			}
		}

		long count = activityRepo.count(); 
		String id = IdGenarator.generateId(request.title(), count);
		while (activityRepo.existsById(id)) {
			count++;
			id = IdGenarator.generateId(request.title(), count);
		} 

		LocalDate schedDate = request.scheduledDate() != null ? request.scheduledDate() : LocalDate.now();
		String status = (request.status() != null && !request.status().trim().isEmpty()) ? request.status().trim().toUpperCase() : "SCHEDULED";
		String priority = (request.priority() != null && !request.priority().trim().isEmpty()) ? request.priority().trim().toUpperCase() : "MEDIUM";

		Activity activity = new Activity(id, request.title(), request.type(), LocalDate.now(), request.notes(),
				schedDate, request.dueDate(), status, priority, project);
		
		return ResponseEntity.status(201).body(new ApiResponse<>(activityRepo.save(activity), "Activity scheduled/recorded successfully ✅", true, Instant.now())); 
	} 

	public ResponseEntity<ApiResponse<Activity>> recordActivity(CreateActivityRequest request) {
		return recordActivity(request, null);
	}

	public ResponseEntity<ApiResponse<Activity>> updateActivity(String id, UpdateActivityRequest request, User currentUser) {
		Activity activity = activityRepo.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Activity not found with ID: " + id));

		if (request.title() != null && !request.title().trim().isEmpty()) {
			activity.setTitle(request.title().trim());
		}
		if (request.type() != null && !request.type().trim().isEmpty()) {
			activity.setType(request.type().trim());
		}
		if (request.notes() != null) {
			activity.setNotes(request.notes().trim());
		}
		if (request.scheduledDate() != null) {
			activity.setScheduledDate(request.scheduledDate());
		}
		if (request.dueDate() != null) {
			activity.setDueDate(request.dueDate());
		}
		if (request.status() != null && !request.status().trim().isEmpty()) {
			activity.setStatus(request.status().trim().toUpperCase());
			if ("COMPLETED".equalsIgnoreCase(request.status().trim())) {
				activity.setCompletedOn(LocalDate.now());
			}
		}
		if (request.priority() != null && !request.priority().trim().isEmpty()) {
			activity.setPriority(request.priority().trim().toUpperCase());
		}

		Activity saved = activityRepo.save(activity);
		return ResponseEntity.ok(new ApiResponse<>(saved, "Activity updated successfully ✅", true, Instant.now()));
	}

	public ResponseEntity<ApiResponse<Activity>> updateActivity(String id, UpdateActivityRequest request) {
		return updateActivity(id, request, null);
	}

	public ResponseEntity<ApiResponse<Activity>> updateActivityStatus(String id, String status) {
		Activity activity = activityRepo.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Activity not found with ID: " + id));

		String newStatus = (status != null && !status.trim().isEmpty()) ? status.trim().toUpperCase() : "COMPLETED";
		activity.setStatus(newStatus);
		if ("COMPLETED".equalsIgnoreCase(newStatus)) {
			activity.setCompletedOn(LocalDate.now());
		}

		Activity saved = activityRepo.save(activity);
		return ResponseEntity.ok(new ApiResponse<>(saved, "Activity status updated to " + newStatus + " ✅", true, Instant.now()));
	}

	public ResponseEntity<ApiResponse<Void>> deleteActivity(String id, User currentUser) {
		if ("SUPERVISOR".equalsIgnoreCase(currentUser.getRole())) {
			return ResponseEntity.status(403).body(new ApiResponse<>(null, "Access Denied: Supervisors cannot delete activity logs.", false, Instant.now()));
		}

		Activity activity = activityRepo.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Activity not found with ID: " + id));

		activityRepo.delete(activity);
		return ResponseEntity.ok(new ApiResponse<>(null, "Activity deleted successfully", true, Instant.now()));
	}

	public ResponseEntity<ApiResponse<Void>> deleteActivity(String id) {
		return deleteActivity(id, null);
	}
}
