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
		while (categoryRepo.existsById(id)) {
			count++;
			id = IdGenarator.generateId(request.name(), count);
		}
		
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

	public ResponseEntity<ApiResponse<Category>> updateCategory(String id, CategoryRequest request) {
		Category category = categoryRepo.findById(id)
				.orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Category not found with ID: " + id));

		String newName = request.name() != null ? request.name().trim() : "";
		if (!newName.isEmpty() && !newName.equalsIgnoreCase(category.getName())) {
			if (categoryRepo.existsByNameIgnoreCase(newName)) {
				return ResponseEntity.status(400).body(new ApiResponse<>(null, "A category with the name '" + newName + "' already exists!", false, Instant.now()));
			}
			category.setName(newName);
		}

		if (request.description() != null) {
			category.setDescription(request.description().trim());
		}

		Category saved = categoryRepo.save(category);
		return ResponseEntity.ok(new ApiResponse<>(saved, "Category updated successfully ✅", true, Instant.now()));
	}

	public ResponseEntity<ApiResponse<Void>> deleteCategory(String id) {
		Category category = categoryRepo.findById(id)
				.orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Category not found with ID: " + id));

		categoryRepo.delete(category);
		return ResponseEntity.ok(new ApiResponse<>(null, "Category deleted successfully", true, Instant.now()));
	}
}
