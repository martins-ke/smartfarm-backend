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
	private final com.smartfarm.user.UserRepository userRepo;
	
	public CategoryService(CategoryRepository categoryRepo, com.smartfarm.user.UserRepository userRepo){
		this.categoryRepo = categoryRepo;
		this.userRepo = userRepo;
	}

	public ResponseEntity<ApiResponse<Category>> createCategory(CategoryRequest request, String userId, String userRole){ 
		if ("SUPERVISOR".equalsIgnoreCase(userRole)) {
			return ResponseEntity.status(403).body(new ApiResponse<>(null, "Supervisors are not permitted to create categories.", false, Instant.now()));
		}
		if ("MANAGER".equalsIgnoreCase(userRole) && userId != null && !userId.trim().isEmpty()) {
			com.smartfarm.user.User manager = userRepo.findById(userId.trim()).orElse(null);
			if (manager == null || manager.getPrivileges() == null || !manager.getPrivileges().contains("CAN_CREATE_CATEGORIES")) {
				return ResponseEntity.status(403).body(new ApiResponse<>(null, "Access Denied: You do not have privilege to create categories.", false, Instant.now()));
			}
		}

		long count = categoryRepo.count();
		String id = IdGenarator.generateId(request.name(), count);
		while (categoryRepo.existsById(id)) {
			count++;
			id = IdGenarator.generateId(request.name(), count);
		}
		
		Category c = new Category(id, request.name(), request.description()); 
		Category saved = categoryRepo.save(c);

		// If manager created it, automatically assign it to their assignedCategories
		if ("MANAGER".equalsIgnoreCase(userRole) && userId != null && !userId.trim().isEmpty()) {
			com.smartfarm.user.User manager = userRepo.findById(userId.trim()).orElse(null);
			if (manager != null) {
				manager.getAssignedCategories().add(saved);
				userRepo.save(manager);
			}
		}

		return ResponseEntity.status(201).body(new ApiResponse<>(saved, "Category created successfully ✅", true, Instant.now())); 
	} 

	public ResponseEntity<ApiResponse<Category>> createCategory(CategoryRequest request) {
		return createCategory(request, null, null);
	} 
	
	public ResponseEntity<ApiResponse<List<Category>>> getAllCategories(String userId, String userRole) { 
		if ("SUPERVISOR".equalsIgnoreCase(userRole) && userId != null && !userId.trim().isEmpty()) {
			List<Category> supCats = categoryRepo.findCategoriesForSupervisor(userId.trim());
			return ResponseEntity.status(200).body(new ApiResponse<>(supCats, null, true, Instant.now()));
		} else if ("MANAGER".equalsIgnoreCase(userRole) && userId != null && !userId.trim().isEmpty()) {
			List<Category> managerCats = categoryRepo.findCategoriesForManager(userId.trim());
			return ResponseEntity.status(200).body(new ApiResponse<>(managerCats, null, true, Instant.now()));
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
