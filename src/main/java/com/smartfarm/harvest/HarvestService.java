package com.smartfarm.harvest;


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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HarvestService {
	private static final Logger log = LoggerFactory.getLogger(HarvestService.class);
	private final HarvestRepository harvestRepo;
	private final ProjectRepository projectRepo;
	private final HarvestInventoryRepository harvestInventoryRepo;
	
	public HarvestService(HarvestRepository harvestRepo, ProjectRepository projectRepo, com.smartfarm.user.UserRepository userRepo, HarvestInventoryRepository harvestInventoryRepo) {
		this.harvestRepo = harvestRepo;
		this.projectRepo = projectRepo;
		this.harvestInventoryRepo = harvestInventoryRepo;
	}
	
	@Transactional
	public ResponseEntity<ApiResponse<Harvest>> recordHarvest(CreateHarvestRequest request, User currentUser){ 
		Project project = projectRepo.findById(request.project_id()).orElseThrow(()-> new EntityNotFoundException("Project not in the system!"));

		if ("SUPERVISOR".equalsIgnoreCase(currentUser.getRole())) {
			boolean isAssigned = project.getSupervisor() != null && currentUser != null && currentUser.getId().trim().equals(project.getSupervisor().getId());
			if (!isAssigned) {
				return ResponseEntity.status(403).body(new ApiResponse<>(null, "Access Denied: You are not assigned to supervise this project.", false, Instant.now()));
			}
			if (currentUser != null) {
				com.smartfarm.user.User sup = currentUser;
				if (sup == null || sup.getPrivileges() == null || !sup.getPrivileges().contains("CAN_RECORD_HARVEST")) {
					return ResponseEntity.status(403).body(new ApiResponse<>(null, "Access Denied: You do not have privilege to record harvest yields.", false, Instant.now()));
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

		long count = harvestRepo.count();
		String id = IdGenarator.generateId(request.item(), count);
		int attempts = 0;
		while (harvestRepo.existsById(id) && attempts < 500) {
			count++;
			attempts++;
			id = IdGenarator.generateId(request.item(), count);
		}
		if (attempts >= 500) {
			log.error("ID generation exceeded 500 attempts for item: {}", request.item());
			throw new IllegalStateException("Unable to generate unique harvest ID. Please try again.");
		}
		Harvest harvest = new Harvest(id, request.item(), request.quantity(), request.units(), request.notes(), LocalDate.now(), project);
        String baseUnit = request.base_unit() != null && !request.base_unit().isBlank() ? request.base_unit() : request.units();
        String displayUnit = request.display_unit() != null && !request.display_unit().isBlank() ? request.display_unit() : request.units();

        HarvestInventory inventoryEntry = harvestInventoryRepo
                .findByProjectNameAndItemName(project.getName(), request.item().trim())
                .orElse(null);
        if (inventoryEntry != null) {
            float newQuantity = inventoryEntry.getAvailableQuantity() + request.quantity();
            inventoryEntry.setAvailableQuantity(newQuantity);
            // Update units metadata if changed
            if (baseUnit != null) inventoryEntry.setBaseUnit(baseUnit);
            if (displayUnit != null) inventoryEntry.setDisplayUnit(displayUnit);
        } else {
            inventoryEntry = new HarvestInventory(request.item().trim(), request.quantity(), request.units(), project.getName(), baseUnit, displayUnit);
        }
        harvestInventoryRepo.save(inventoryEntry);
		
		log.info("Harvest recorded: item={}, quantity={}, project={}", request.item(), request.quantity(), project.getName());
		return ResponseEntity.status(201).body(new ApiResponse<>(harvestRepo.save(harvest), "Harvest recorded successfully ✅", true, Instant.now())); 
	}

	public ResponseEntity<ApiResponse<Harvest>> recordHarvest(CreateHarvestRequest request) {
		return recordHarvest(request, null);
	}

	@Transactional
	public ResponseEntity<ApiResponse<Harvest>> updateHarvest(String id, UpdateHarvestRequest request, User currentUser) {
		Harvest harvest = harvestRepo.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Harvest not found with ID: " + id));

		final String oldItem = harvest.getItem() != null ? harvest.getItem().trim() : "";
		final float oldQuantity = harvest.getQuantity();
		String rawProjectName = harvest.getProject() != null ? harvest.getProject().getName() : null;
		if (rawProjectName == null && harvest.getProjectId() != null) {
			rawProjectName = projectRepo.findById(harvest.getProjectId()).map(Project::getName).orElse(null);
		}
		final String projectName = rawProjectName;

		final String newItem = (request.item() != null && !request.item().trim().isEmpty()) ? request.item().trim() : oldItem;
		final float newQuantity = (request.quantity() != null && request.quantity() > 0) ? request.quantity() : oldQuantity;

		if (projectName != null && harvestInventoryRepo != null) {
			// If item name changed, rollback old item inventory and credit new item inventory
			if (!oldItem.equalsIgnoreCase(newItem)) {
				harvestInventoryRepo.findByProjectNameAndItemName(projectName, oldItem)
					.ifPresent(oldInv -> {
						oldInv.setAvailableQuantity(Math.max(0f, oldInv.getAvailableQuantity() - oldQuantity));
						harvestInventoryRepo.save(oldInv);
					});

				HarvestInventory newInv = harvestInventoryRepo.findByProjectNameAndItemName(projectName, newItem)
					.orElseGet(() -> new HarvestInventory(newItem, 0f, request.units(), projectName, null, null));
				newInv.setAvailableQuantity(newInv.getAvailableQuantity() + newQuantity);
				if (request.units() != null) newInv.setUnits(request.units());
				harvestInventoryRepo.save(newInv);
			} else if (oldQuantity != newQuantity) {
				// Same item, adjust quantity delta
				final float delta = newQuantity - oldQuantity;
				harvestInventoryRepo.findByProjectNameAndItemName(projectName, newItem)
					.ifPresent(inv -> {
						float updated = Math.max(0f, inv.getAvailableQuantity() + delta);
						inv.setAvailableQuantity(updated);
						harvestInventoryRepo.save(inv);
						log.info("Adjusted harvest inventory for '{}' in project '{}'. Delta: {}, New available: {}",
							newItem, projectName, delta, updated);
					});
			}
		}

		if (request.item() != null && !request.item().trim().isEmpty()) {
			harvest.setItem(newItem);
		}
		if (request.quantity() != null && request.quantity() > 0) {
			harvest.setQuantity(newQuantity);
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
		return updateHarvest(id, request, null);
	}

	@Transactional
	public ResponseEntity<ApiResponse<Void>> deleteHarvest(String id, User currentUser) {
		if ("SUPERVISOR".equalsIgnoreCase(currentUser.getRole())) {
			return ResponseEntity.status(403).body(new ApiResponse<>(null, "Access Denied: Supervisors cannot delete harvest logs.", false, Instant.now()));
		}

		Harvest harvest = harvestRepo.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Harvest not found with ID: " + id));

		// Roll back inventory atomically
		String rawProjectName = harvest.getProject() != null ? harvest.getProject().getName() : null;
		if (rawProjectName == null && harvest.getProjectId() != null) {
			rawProjectName = projectRepo.findById(harvest.getProjectId()).map(Project::getName).orElse(null);
		}
		final String projectName = rawProjectName;
		final String itemName = harvest.getItem() != null ? harvest.getItem().trim() : "";
		final float harvestQty = harvest.getQuantity();

		if (projectName != null && !itemName.isEmpty() && harvestInventoryRepo != null) {
			harvestInventoryRepo.findByProjectNameAndItemName(projectName, itemName)
				.ifPresent(inv -> {
					float remaining = Math.max(0f, inv.getAvailableQuantity() - harvestQty);
					inv.setAvailableQuantity(remaining);
					harvestInventoryRepo.save(inv);
					log.info("Rolled back harvest inventory for '{}' in project '{}'. Deducted: {}, Remaining: {}",
						itemName, projectName, harvestQty, remaining);
				});
		}

		harvestRepo.delete(harvest);
		return ResponseEntity.ok(new ApiResponse<>(null, "Harvest deleted successfully", true, Instant.now()));
	}

	public ResponseEntity<ApiResponse<Void>> deleteHarvest(String id) {
		return deleteHarvest(id, null);
	}
	
    public ResponseEntity<ApiResponse<java.util.List<HarvestInventory>>> getHarvestInventory() {
        return ResponseEntity.status(200).body(new ApiResponse<>(
            harvestInventoryRepo.findAll(Sort.by("projectName", "itemName")),
            "Available stock items",
            true,
            Instant.now()
        ));
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
