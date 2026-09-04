package com.smartfarm.activities;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartfarm.ApiResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/activities")
public class ActivityController {
	private final ActivityService activityService;
	
	public ActivityController(ActivityService activityService) {
		this.activityService = activityService;
	}
	
	@PostMapping("/record")
	public ResponseEntity<ApiResponse<Activity>> recordActivity(@Valid @RequestBody CreateActivityRequest request){
		return activityService.recordActivity(request);
	}

	@org.springframework.web.bind.annotation.PutMapping("/{id}")
	public ResponseEntity<ApiResponse<Activity>> updateActivity(
			@org.springframework.web.bind.annotation.PathVariable String id,
			@Valid @RequestBody UpdateActivityRequest request) {
		return activityService.updateActivity(id, request);
	}

	@org.springframework.web.bind.annotation.DeleteMapping("/{id}")
	public ResponseEntity<ApiResponse<Void>> deleteActivity(@org.springframework.web.bind.annotation.PathVariable String id) {
		return activityService.deleteActivity(id);
	}
}
