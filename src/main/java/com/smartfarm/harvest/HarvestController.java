package com.smartfarm.harvest;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartfarm.ApiResponse;
import com.smartfarm.user.User;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/harvest")
public class HarvestController {
	private final HarvestService harvestService;
	
	public HarvestController(HarvestService harvestService) {
		this.harvestService = harvestService;
	}
	
	@PostMapping("/record")
	public ResponseEntity<ApiResponse<Harvest>> recordHarvest(
			@Valid @RequestBody CreateHarvestRequest request,
			@AuthenticationPrincipal User currentUser) {
		return harvestService.recordHarvest(request, currentUser); 
	}

	@PutMapping("/{id}")
	public ResponseEntity<ApiResponse<Harvest>> updateHarvest(
			@PathVariable String id,
			@Valid @RequestBody UpdateHarvestRequest request,
			@AuthenticationPrincipal User currentUser) {
		return harvestService.updateHarvest(id, request, currentUser);
	}

	@GetMapping("/project/{projectId}")
	public ResponseEntity<ApiResponse<List<Harvest>>> getHarvestByProjectId(
			@PathVariable String projectId) {
		return harvestService.getHarvestByProjectId(projectId);
	}

	@GetMapping("/{id}")
	public ResponseEntity<ApiResponse<Harvest>> getHarvestById(
			@PathVariable String id) {
		return harvestService.getHarvestById(id);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<ApiResponse<Void>> deleteHarvest(
			@PathVariable String id,
			@AuthenticationPrincipal User currentUser) {
		return harvestService.deleteHarvest(id, currentUser);
	}

	@GetMapping("/inventory")
	public ResponseEntity<ApiResponse<List<HarvestInventory>>> getHarvestInventory() {
		return harvestService.getHarvestInventory(); 
	}
}
