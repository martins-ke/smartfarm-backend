package com.smartfarm.category;

import java.time.Instant;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.smartfarm.ApiResponse;
import com.smartfarm.util.IdGenarator;

@Service
public class CategoryService {
	private final CategoryRepository categoryRepo;
	
	public CategoryService(CategoryRepository categoryRepo){
		this.categoryRepo = categoryRepo;
	}

	public ResponseEntity<ApiResponse<Category>> createCategory(CategoryRequest request){ 
		long count = categoryRepo.count();
		String id = IdGenarator.generateId(request.name(), count);
		
		Category c = new Category(id, request.name(), request.description()); 
		
		return ResponseEntity.status(201).body(new ApiResponse<>(categoryRepo.save(c), "Category created successfully ✅", true, Instant.now())); 
	} 
	
	public ResponseEntity<ApiResponse<List<Category>>> getAllCategories(String userId, String userRole) { 
		if ("SUPERVISOR".equalsIgnoreCase(userRole) && userId != null && !userId.trim().isEmpty()) {
			List<Category> supCats = categoryRepo.findCategoriesForSupervisor(userId.trim());
			return ResponseEntity.status(200).body(new ApiResponse<>(supCats, null, true, Instant.now()));
		} else if ("MANAGER".equalsIgnoreCase(userRole) && userId != null && !userId.trim().isEmpty()) {
			List<Category> managerCats = categoryRepo.findCategoriesForManager(userId.trim());
			if (!managerCats.isEmpty()) {
				return ResponseEntity.status(200).body(new ApiResponse<>(managerCats, null, true, Instant.now()));
			}
		}
		return ResponseEntity.status(200).body(new ApiResponse<>(categoryRepo.findAll(), null, true, Instant.now())); 
	}
}
