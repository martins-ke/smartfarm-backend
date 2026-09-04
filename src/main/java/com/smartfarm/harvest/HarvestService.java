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
		while (harvestRepo.existsById(id)) {
			count++;
			id = IdGenarator.generateId(request.item(), count);
		}
		Harvest harvest = new Harvest(id, request.item(), request.quantity(), request.units(), request.notes(), LocalDate.now(), project);
		
		return ResponseEntity.status(201).body(new ApiResponse<>(harvestRepo.save(harvest), "Harvest recorded successfully ✅", true, Instant.now())); 
	}

	public ResponseEntity<ApiResponse<Harvest>> updateHarvest(String id, UpdateHarvestRequest request) {
		Harvest harvest = harvestRepo.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Harvest not found with ID: " + id));

		if (request.item() != null && !request.item().trim().isEmpty()) {
			harvest.setItem(request.item().trim());
		}
		if (request.quantity() != null && request.quantity() > 0) {
			harvest.setQuantity(request.quantity());
		}
		if (request.units() != null && !request.units().trim().isEmpty()) {
			harvest.setUnits(request.units().trim());
		}
		if (request.notes() != null) {
			harvest.setNotes(request.notes().trim());
		}

		Harvest saved = harvestRepo.save(harvest);
		return ResponseEntity.ok(new ApiResponse<>(saved, "Harvest updated successfully ✅", true, Instant.now()));
	}

	public ResponseEntity<ApiResponse<Void>> deleteHarvest(String id) {
		Harvest harvest = harvestRepo.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Harvest not found with ID: " + id));

		harvestRepo.delete(harvest);
		return ResponseEntity.ok(new ApiResponse<>(null, "Harvest deleted successfully", true, Instant.now()));
	}
}
