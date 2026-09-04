package com.smartfarm.category;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smartfarm.ApiResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/categories")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class CategoryController {
	private final CategoryService categoryService;
	
	public CategoryController(CategoryService categoryService) {
		this.categoryService = categoryService;
	}
	
	@PostMapping("/create")
	public ResponseEntity<ApiResponse<Category>> createCategory(
			@Valid @RequestBody CategoryRequest request,
			@RequestHeader(value = "X-User-Id", required = false) String headerUserId,
			@RequestHeader(value = "X-User-Role", required = false) String headerUserRole,
			@RequestParam(required = false) String userId,
			@RequestParam(required = false) String userRole){
		String effectiveUserId = userId != null ? userId : headerUserId;
		String effectiveUserRole = userRole != null ? userRole : headerUserRole;
		return categoryService.createCategory(request, effectiveUserId, effectiveUserRole); 
	}
	
	@GetMapping("/all")
	public ResponseEntity<ApiResponse<List<Category>>> getAllCategories(
			@RequestHeader(value = "X-User-Id", required = false) String headerUserId,
			@RequestHeader(value = "X-User-Role", required = false) String headerUserRole,
			@RequestParam(required = false) String userId,
			@RequestParam(required = false) String userRole) {
		
		String effectiveUserId = userId != null ? userId : headerUserId;
		String effectiveUserRole = userRole != null ? userRole : headerUserRole;

		return categoryService.getAllCategories(effectiveUserId, effectiveUserRole); 
	}

	@org.springframework.web.bind.annotation.PutMapping("/{id}")
	public ResponseEntity<ApiResponse<Category>> updateCategory(
			@PathVariable String id,
			@Valid @RequestBody CategoryRequest request) {
		return categoryService.updateCategory(id, request);
	}

	@org.springframework.web.bind.annotation.DeleteMapping("/{id}")
	public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable String id) {
		return categoryService.deleteCategory(id);
	}
}
