package com.smartfarm.activities;

import java.time.Instant;
import java.time.LocalDate;

import org.springframework.http.ResponseEntity;
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
	private final com.smartfarm.user.UserRepository userRepo;
	
	public ActivityService(ActivityRepository activityRepo, ProjectRepository projectRepo, com.smartfarm.user.UserRepository userRepo) {
		this.activityRepo = activityRepo;
		this.projectRepo = projectRepo;
		this.userRepo = userRepo;
	}
	
	public ResponseEntity<ApiResponse<Activity>> recordActivity(CreateActivityRequest request, String userId, String userRole){
		Project project = projectRepo.findById(request.project_id()).orElseThrow(()-> new EntityNotFoundException("Project not in the system!"));

		if ("SUPERVISOR".equalsIgnoreCase(userRole)) {
			boolean isAssigned = project.getSupervisor() != null && userId != null && userId.trim().equals(project.getSupervisor().getId());
			if (!isAssigned) {
				return ResponseEntity.status(403).body(new ApiResponse<>(null, "Access Denied: You are not assigned to supervise this project.", false, Instant.now()));
			}
			if (userId != null) {
				com.smartfarm.user.User sup = userRepo.findById(userId.trim()).orElse(null);
				if (sup == null || sup.getPrivileges() == null || !sup.getPrivileges().contains("CAN_LOG_ACTIVITIES")) {
					return ResponseEntity.status(403).body(new ApiResponse<>(null, "Access Denied: You do not have privilege to log daily field activities.", false, Instant.now()));
				}
			}
		} else if ("MANAGER".equalsIgnoreCase(userRole) && userId != null && !userId.trim().isEmpty()) {
			com.smartfarm.user.User manager = userRepo.findById(userId.trim()).orElse(null);
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
		Activity activity = new Activity(id, request.title(), request.type(),LocalDate.now(), request.notes(), project);
		
		return ResponseEntity.status(201).body(new ApiResponse<>(activityRepo.save(activity), "Activity recorded successfully ✅", true, Instant.now())); 
	} 

	public ResponseEntity<ApiResponse<Activity>> recordActivity(CreateActivityRequest request) {
		return recordActivity(request, null, null);
	}

	public ResponseEntity<ApiResponse<Activity>> updateActivity(String id, UpdateActivityRequest request, String userId, String userRole) {
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

		Activity saved = activityRepo.save(activity);
		return ResponseEntity.ok(new ApiResponse<>(saved, "Activity updated successfully ✅", true, Instant.now()));
	}

	public ResponseEntity<ApiResponse<Activity>> updateActivity(String id, UpdateActivityRequest request) {
		return updateActivity(id, request, null, null);
	}

	public ResponseEntity<ApiResponse<Void>> deleteActivity(String id, String userId, String userRole) {
		if ("SUPERVISOR".equalsIgnoreCase(userRole)) {
			return ResponseEntity.status(403).body(new ApiResponse<>(null, "Access Denied: Supervisors cannot delete activity logs.", false, Instant.now()));
		}

		Activity activity = activityRepo.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Activity not found with ID: " + id));

		activityRepo.delete(activity);
		return ResponseEntity.ok(new ApiResponse<>(null, "Activity deleted successfully", true, Instant.now()));
	}

	public ResponseEntity<ApiResponse<Void>> deleteActivity(String id) {
		return deleteActivity(id, null, null);
	}
}
