package com.smartfarm.season;

import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;


import com.smartfarm.ApiResponse;

@Service
public class SeasonService {
	@Autowired SeasonRepository seasonRepo;
	private static final String STATUS = "active";

	public ResponseEntity<ApiResponse<?>> createSeason(CreateSeasonRequest request){
		
		Season s = new Season(request.name(), request.size(), request.period(), request.budget(), STATUS);
		
		return ResponseEntity.status(200).body(new ApiResponse<>(seasonRepo.save(s), "Season created successfully ✅", true,Instant.now())); 
	}
	
	public ResponseEntity<ApiResponse<Long>> allSeasons(){
		
		return ResponseEntity.status(200).body(new ApiResponse<>(seasonRepo.count(), null, true,Instant.now())); 
	}
	public ResponseEntity<ApiResponse<Long>> completeSeasons(){
		Long count = seasonRepo.findByStatus("complete");
		if(count == null) return ResponseEntity.status(400).body(new ApiResponse<>(0L, "No complete seasons yet!", true,Instant.now()));;
			
		return ResponseEntity.status(200).body(new ApiResponse<>(count, null, true,Instant.now())); 
	}
	
	public ResponseEntity<ApiResponse<List<Season>>> getSeasons() {
			
			return ResponseEntity.status(200).body(new ApiResponse<>(seasonRepo.findAll(), null, true,Instant.now()));
	}
}
