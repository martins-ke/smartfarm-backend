package com.smartfarm.harvest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartfarm.ApiResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/harvest")
final class HarvestController {
	private final HarvestService harvestService;
	
	private HarvestController( HarvestService harvestService) {
		this.harvestService = harvestService;
	}
	
	@PostMapping("/record")
	final ResponseEntity<ApiResponse<Harvest>> recordHarvest(
			@Valid @RequestBody CreateHarvestRequest request,
			@org.springframework.web.bind.annotation.RequestHeader(value = "X-User-Id", required = false) String headerUserId,
			@org.springframework.web.bind.annotation.RequestHeader(value = "X-User-Role", required = false) String headerUserRole,
			@org.springframework.web.bind.annotation.RequestParam(required = false) String userId,
			@org.springframework.web.bind.annotation.RequestParam(required = false) String userRole){
		String effectiveUserId = userId != null ? userId : headerUserId;
		String effectiveUserRole = userRole != null ? userRole : headerUserRole;
		return harvestService.recordHarvest(request, effectiveUserId, effectiveUserRole); 
	}

	@org.springframework.web.bind.annotation.PutMapping("/{id}")
	final ResponseEntity<ApiResponse<Harvest>> updateHarvest(
			@org.springframework.web.bind.annotation.PathVariable String id,
			@Valid @RequestBody UpdateHarvestRequest request,
			@org.springframework.web.bind.annotation.RequestHeader(value = "X-User-Id", required = false) String headerUserId,
			@org.springframework.web.bind.annotation.RequestHeader(value = "X-User-Role", required = false) String headerUserRole,
			@org.springframework.web.bind.annotation.RequestParam(required = false) String userId,
			@org.springframework.web.bind.annotation.RequestParam(required = false) String userRole) {
		String effectiveUserId = userId != null ? userId : headerUserId;
		String effectiveUserRole = userRole != null ? userRole : headerUserRole;
		return harvestService.updateHarvest(id, request, effectiveUserId, effectiveUserRole);
	}

	@org.springframework.web.bind.annotation.GetMapping("/project/{projectId}")
	final ResponseEntity<ApiResponse<java.util.List<Harvest>>> getHarvestByProjectId(
			@org.springframework.web.bind.annotation.PathVariable String projectId) {
		return harvestService.getHarvestByProjectId(projectId);
	}

	@org.springframework.web.bind.annotation.GetMapping("/{id}")
	final ResponseEntity<ApiResponse<Harvest>> getHarvestById(
			@org.springframework.web.bind.annotation.PathVariable String id) {
		return harvestService.getHarvestById(id);
	}

	@org.springframework.web.bind.annotation.DeleteMapping("/{id}")
	final ResponseEntity<ApiResponse<Void>> deleteHarvest(
			@org.springframework.web.bind.annotation.PathVariable String id,
			@org.springframework.web.bind.annotation.RequestHeader(value = "X-User-Id", required = false) String headerUserId,
			@org.springframework.web.bind.annotation.RequestHeader(value = "X-User-Role", required = false) String headerUserRole,
			@org.springframework.web.bind.annotation.RequestParam(required = false) String userId,
			@org.springframework.web.bind.annotation.RequestParam(required = false) String userRole) {
		String effectiveUserId = userId != null ? userId : headerUserId;
		String effectiveUserRole = userRole != null ? userRole : headerUserRole;
		return harvestService.deleteHarvest(id, effectiveUserId, effectiveUserRole);
	}
}
