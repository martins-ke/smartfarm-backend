package com.smartfarm.expenses;

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
@RequestMapping("/expenses")
public class ExpenseController {
	
	private final ExpenseService expenseService;
	
	public ExpenseController(ExpenseService expenseService) {
		this.expenseService = expenseService;
	}
	
	@PostMapping("/create")
	public ResponseEntity<ApiResponse<Expense>> createExpense(
			@Valid @RequestBody CreateExpenseRequest request,
			@AuthenticationPrincipal User currentUser) {
		return expenseService.createExpense(request, currentUser); 
	}
	
	@GetMapping("/{project_id}")
	public ResponseEntity<ApiResponse<List<Expense>>> getExpensesByProjectId(
			@PathVariable String project_id,
			@AuthenticationPrincipal User currentUser) {
		return expenseService.getExpensesByProjectId(project_id, currentUser); 
	}

	@PutMapping("/{id}")
	public ResponseEntity<ApiResponse<Expense>> updateExpense(
			@PathVariable String id,
			@Valid @RequestBody UpdateExpenseRequest request,
			@AuthenticationPrincipal User currentUser) {
		return expenseService.updateExpense(id, request, currentUser);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<ApiResponse<Void>> deleteExpense(
			@PathVariable String id,
			@AuthenticationPrincipal User currentUser) {
		return expenseService.deleteExpense(id, currentUser);
	}
}
