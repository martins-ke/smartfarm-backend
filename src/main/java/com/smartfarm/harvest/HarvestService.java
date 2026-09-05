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
	private final com.smartfarm.user.UserRepository userRepo;
	
	public HarvestService(HarvestRepository harvestRepo, ProjectRepository projectRepo, com.smartfarm.user.UserRepository userRepo) {
		this.harvestRepo = harvestRepo;
		this.projectRepo = projectRepo;
		this.userRepo = userRepo;
	}
	
	public ResponseEntity<ApiResponse<Harvest>> recordHarvest(CreateHarvestRequest request, String userId, String userRole){
		Project project = projectRepo.findById(request.project_id()).orElseThrow(()-> new EntityNotFoundException("Project not in the system!"));

		if ("SUPERVISOR".equalsIgnoreCase(userRole)) {
			boolean isAssigned = project.getSupervisor() != null && userId != null && userId.trim().equals(project.getSupervisor().getId());
			if (!isAssigned) {
				return ResponseEntity.status(403).body(new ApiResponse<>(null, "Access Denied: You are not assigned to supervise this project.", false, Instant.now()));
			}
			if (userId != null) {
				com.smartfarm.user.User sup = userRepo.findById(userId.trim()).orElse(null);
				if (sup == null || sup.getPrivileges() == null || !sup.getPrivileges().contains("CAN_RECORD_HARVEST")) {
					return ResponseEntity.status(403).body(new ApiResponse<>(null, "Access Denied: You do not have privilege to record harvest yields.", false, Instant.now()));
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

		long count = harvestRepo.count(); 
		String id = IdGenarator.generateId(request.item(), count);
		while (harvestRepo.existsById(id)) {
			count++;
			id = IdGenarator.generateId(request.item(), count);
		}
		Harvest harvest = new Harvest(id, request.item(), request.quantity(), request.units(), request.notes(), LocalDate.now(), project);
		
		return ResponseEntity.status(201).body(new ApiResponse<>(harvestRepo.save(harvest), "Harvest recorded successfully ✅", true, Instant.now())); 
	}

	public ResponseEntity<ApiResponse<Harvest>> recordHarvest(CreateHarvestRequest request) {
		return recordHarvest(request, null, null);
	}

	public ResponseEntity<ApiResponse<Harvest>> updateHarvest(String id, UpdateHarvestRequest request, String userId, String userRole) {
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

	public ResponseEntity<ApiResponse<Harvest>> updateHarvest(String id, UpdateHarvestRequest request) {
		return updateHarvest(id, request, null, null);
	}

	public ResponseEntity<ApiResponse<Void>> deleteHarvest(String id, String userId, String userRole) {
		if ("SUPERVISOR".equalsIgnoreCase(userRole)) {
			return ResponseEntity.status(403).body(new ApiResponse<>(null, "Access Denied: Supervisors cannot delete harvest logs.", false, Instant.now()));
		}

		Harvest harvest = harvestRepo.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Harvest not found with ID: " + id));

		harvestRepo.delete(harvest);
		return ResponseEntity.ok(new ApiResponse<>(null, "Harvest deleted successfully", true, Instant.now()));
	}

	public ResponseEntity<ApiResponse<Void>> deleteHarvest(String id) {
		return deleteHarvest(id, null, null);
	}

	public ResponseEntity<ApiResponse<java.util.List<Harvest>>> getHarvestByProjectId(String projectId) {
		java.util.List<Harvest> list = harvestRepo.findByProjectId(projectId);
		return ResponseEntity.ok(new ApiResponse<>(list, "Harvest records retrieved successfully", true, Instant.now()));
	}

	public ResponseEntity<ApiResponse<Harvest>> getHarvestById(String id) {
		Harvest harvest = harvestRepo.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Harvest not found with ID: " + id));
		return ResponseEntity.ok(new ApiResponse<>(harvest, "Harvest record retrieved successfully", true, Instant.now()));
	}
}
