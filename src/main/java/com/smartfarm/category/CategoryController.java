package com.smartfarm.category;

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
@RequestMapping("/categories")
public class CategoryController {
	private final CategoryService categoryService;
	
	public CategoryController(CategoryService categoryService) {
		this.categoryService = categoryService;
	}
	
	@PostMapping("/create")
	public ResponseEntity<ApiResponse<Category>> createCategory(
			@Valid @RequestBody CategoryRequest request,
			@AuthenticationPrincipal User currentUser) {
		return categoryService.createCategory(request, currentUser); 
	}
	
	@GetMapping("/all")
	public ResponseEntity<ApiResponse<List<Category>>> getAllCategories(
			@AuthenticationPrincipal User currentUser) {
		return categoryService.getAllCategories(currentUser); 
	}

	@PutMapping("/{id}")
	public ResponseEntity<ApiResponse<Category>> updateCategory(
			@PathVariable String id,
			@Valid @RequestBody CategoryRequest request,
			@AuthenticationPrincipal User currentUser) {
		return categoryService.updateCategory(id, request, currentUser);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<ApiResponse<Void>> deleteCategory(
			@PathVariable String id,
			@AuthenticationPrincipal User currentUser) {
		return categoryService.deleteCategory(id, currentUser);
	}
}
