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
	
	public ActivityService(ActivityRepository activityRepo, ProjectRepository projectRepo) {
		this.activityRepo = activityRepo;
		this.projectRepo = projectRepo;
	}
	
	public ResponseEntity<ApiResponse<Activity>> recordActivity(CreateActivityRequest request){
		Project project = projectRepo.findById(request.project_id()).orElseThrow(()-> new EntityNotFoundException("Project not in the system!"));
		long count = activityRepo.count(); 
		String id = IdGenarator.generateId(request.title(), count);
		while (activityRepo.existsById(id)) {
			count++;
			id = IdGenarator.generateId(request.title(), count);
		} 
		Activity activity = new Activity(id, request.title(), request.type(),LocalDate.now(), request.notes(), project);
		
		return ResponseEntity.status(201).body(new ApiResponse<>(activityRepo.save(activity), "Activity recorded successfully ✅", true, Instant.now())); 
	} 
}
