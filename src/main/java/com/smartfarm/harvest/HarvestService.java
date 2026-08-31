package com.smartfarm.harvest;


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
public class HarvestService {
	private final HarvestRepository harvestRepo;
	private final ProjectRepository projectRepo;
	
	public HarvestService(HarvestRepository harvestRepo, ProjectRepository projectRepo) {
		this.harvestRepo = harvestRepo;
		this.projectRepo = projectRepo;
	}
	
	public ResponseEntity<ApiResponse<Harvest>> recordHarvest(CreateHarvestRequest request){
		Project project = projectRepo.findById(request.project_id()).orElseThrow(()-> new EntityNotFoundException("Project not in the system!"));
		long count = harvestRepo.count(); 
		
		String id = IdGenarator.generateId(request.item(), count);
		Harvest harvest = new Harvest(id, request.item(), request.quantity(), request.units(), request.notes(), LocalDate.now(), project);
		
		return ResponseEntity.status(201).body(new ApiResponse<>(harvestRepo.save(harvest), "Harvest recorded successfully ✅", true, Instant.now())); 
	}

}
