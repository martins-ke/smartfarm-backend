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
	final ResponseEntity<ApiResponse<Harvest>> recordHarvest(@Valid @RequestBody CreateHarvestRequest request){
		
		return harvestService.recordHarvest(request); 
	}

}
