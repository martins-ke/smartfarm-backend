package com.smartfarm.activities;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
import com.smartfarm.user.User;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/activities")
public class ActivityController {
	private final ActivityService activityService;
	
	public ActivityController(ActivityService activityService) {
		this.activityService = activityService;
	}
	
	@PostMapping("/record")
	public ResponseEntity<ApiResponse<Activity>> recordActivity(
			@Valid @RequestBody CreateActivityRequest request,
			@AuthenticationPrincipal User currentUser) {
		return activityService.recordActivity(request, currentUser);
	}

	@PutMapping("/{id}")
	public ResponseEntity<ApiResponse<Activity>> updateActivity(
			@PathVariable String id,
			@Valid @RequestBody UpdateActivityRequest request,
			@AuthenticationPrincipal User currentUser) {
		return activityService.updateActivity(id, request, currentUser);
	}

	@PatchMapping("/{id}/status")
	public ResponseEntity<ApiResponse<Activity>> updateStatus(
			@PathVariable String id,
			@RequestParam(required = false, defaultValue = "COMPLETED") String status) {
		return activityService.updateActivityStatus(id, status);
	}

	@GetMapping("/{id}/labor")
	public ResponseEntity<ApiResponse<List<ActivityLaborAssignment>>> getLaborAssignments(
			@PathVariable String id, 
			@AuthenticationPrincipal User currentUser) {
		return activityService.getLaborAssignments(id);
	}

	@PostMapping("/{id}/labor")
	public ResponseEntity<ApiResponse<?>> assignLabor(
			@PathVariable String id,
			@RequestBody AssignLaborRequest request, 
			@AuthenticationPrincipal User currentUser) {
		return activityService.assignLaborToActivity(id, request);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<ApiResponse<Void>> deleteActivity(
			@PathVariable String id,
			@AuthenticationPrincipal User currentUser) {
		return activityService.deleteActivity(id, currentUser);
	}
}
