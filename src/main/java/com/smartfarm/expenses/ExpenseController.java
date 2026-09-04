package com.smartfarm.expenses;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
	public ResponseEntity<ApiResponse<Expense>> createProject(@Valid @RequestBody CreateExpenseRequest request){
		return expenseService.createExpense(request);
		 
	}
	
	@GetMapping("/{project_id}")
	public ResponseEntity<ApiResponse<List<Expense>>> getExpensesByProjectId(@PathVariable String project_id) {
		return expenseService.getExpensesByProjectId(project_id); 
	}

	@org.springframework.web.bind.annotation.PutMapping("/{id}")
	public ResponseEntity<ApiResponse<Expense>> updateExpense(
			@PathVariable String id,
			@Valid @RequestBody UpdateExpenseRequest request) {
		return expenseService.updateExpense(id, request);
	}

	@org.springframework.web.bind.annotation.DeleteMapping("/{id}")
	public ResponseEntity<ApiResponse<Void>> deleteExpense(@PathVariable String id) {
		return expenseService.deleteExpense(id);
	}
}
