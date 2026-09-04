package com.smartfarm.expenses;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


import com.smartfarm.ApiResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/expenses")
@CrossOrigin(origins = "http://localhost:3000")
public class ExpenseController {
	
	private ExpenseService expenseService;
	
	public ExpenseController(ExpenseService expenseService) {
		this.expenseService = expenseService;
	}
	
	@PostMapping("/create")
	public ResponseEntity<ApiResponse<Expense>> createExpense(
			@Valid @RequestBody CreateExpenseRequest request,
			@RequestHeader(value = "X-User-Id", required = false) String headerUserId,
			@RequestHeader(value = "X-User-Role", required = false) String headerUserRole,
			@RequestParam(required = false) String userId,
			@RequestParam(required = false) String userRole){
		String effectiveUserId = userId != null ? userId : headerUserId;
		String effectiveUserRole = userRole != null ? userRole : headerUserRole;
		return expenseService.createExpense(request, effectiveUserId, effectiveUserRole); 
	}
	
	@GetMapping("/{project_id}")
	public ResponseEntity<ApiResponse<List<Expense>>> getExpensesByProjectId(
			@PathVariable String project_id,
			@RequestHeader(value = "X-User-Id", required = false) String headerUserId,
			@RequestHeader(value = "X-User-Role", required = false) String headerUserRole,
			@RequestParam(required = false) String userId,
			@RequestParam(required = false) String userRole) {
		String effectiveUserId = userId != null ? userId : headerUserId;
		String effectiveUserRole = userRole != null ? userRole : headerUserRole;
		return expenseService.getExpensesByProjectId(project_id, effectiveUserId, effectiveUserRole); 
	}

	@org.springframework.web.bind.annotation.PutMapping("/{id}")
	public ResponseEntity<ApiResponse<Expense>> updateExpense(
			@PathVariable String id,
			@Valid @RequestBody UpdateExpenseRequest request,
			@RequestHeader(value = "X-User-Id", required = false) String headerUserId,
			@RequestHeader(value = "X-User-Role", required = false) String headerUserRole,
			@RequestParam(required = false) String userId,
			@RequestParam(required = false) String userRole) {
		String effectiveUserId = userId != null ? userId : headerUserId;
		String effectiveUserRole = userRole != null ? userRole : headerUserRole;
		return expenseService.updateExpense(id, request, effectiveUserId, effectiveUserRole);
	}

	@org.springframework.web.bind.annotation.DeleteMapping("/{id}")
	public ResponseEntity<ApiResponse<Void>> deleteExpense(
			@PathVariable String id,
			@RequestHeader(value = "X-User-Id", required = false) String headerUserId,
			@RequestHeader(value = "X-User-Role", required = false) String headerUserRole,
			@RequestParam(required = false) String userId,
			@RequestParam(required = false) String userRole) {
		String effectiveUserId = userId != null ? userId : headerUserId;
		String effectiveUserRole = userRole != null ? userRole : headerUserRole;
		return expenseService.deleteExpense(id, effectiveUserId, effectiveUserRole);
	}
}
