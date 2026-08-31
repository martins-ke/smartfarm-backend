package com.smartfarm.season; 

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import com.smartfarm.ApiResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/seasons")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"}) 
public class SeasonController {
	@Autowired SeasonService service; 

	@PostMapping("/new")
	public ResponseEntity<ApiResponse<?>> createSeason(@Valid @RequestBody CreateSeasonRequest request){
		
		return service.createSeason(request); 
	}
	@GetMapping("/all/count")
	public ResponseEntity<ApiResponse<Long>> allSeasons(){
		
		return service.allSeasons(); 
	}
	
	@GetMapping("/complete/count")
	public ResponseEntity<ApiResponse<Long>> completeSeasons(){
		
		return service.completeSeasons(); 
	}
	
	@GetMapping("/all")
	public ResponseEntity<ApiResponse<List<Season>>> getSeasons() {
		
 
		
		return service.getSeasons(); 
	}
	
	
}
