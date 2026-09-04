package com.smartfarm.expenses;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;


import com.smartfarm.ApiResponse;
import com.smartfarm.projects.Project;
import com.smartfarm.projects.ProjectRepository;
import com.smartfarm.util.IdGenarator;

import jakarta.persistence.EntityNotFoundException;

@Service
public class ExpenseService {

	private final ExpenseRepository expenseRepo;
	private final ProjectRepository projectRepo;
	
	public ExpenseService(ExpenseRepository expenseRepo, ProjectRepository projectRepo) {
		this.expenseRepo = expenseRepo;
		this.projectRepo = projectRepo;
	}
	
	public ResponseEntity<ApiResponse<Expense>> createExpense(CreateExpenseRequest request){
		Project project = projectRepo.findById(request.project_id()).orElseThrow(()-> new EntityNotFoundException("Project not in the system!"));
		long count = expenseRepo.count(); 
		String id = IdGenarator.generateId(request.title(), count);
		while (expenseRepo.existsById(id)) {
			count++;
			id = IdGenarator.generateId(request.title(), count);
		} 
		Expense expense = new Expense(id, request.title(), request.amount(), request.unitPrice(), request.quantity(), LocalDate.now(), request.notes(), project);
		
		return ResponseEntity.status(201).body(new ApiResponse<>(expenseRepo.save(expense), "Expense recorded successfully ✅", true, Instant.now())); 
	}
	
	public ResponseEntity<ApiResponse<List<Expense>>> getExpensesByProjectId(String projectId) {
		return ResponseEntity.status(200).body(new ApiResponse<>(expenseRepo.findByProjectId(projectId), "Expense list fetched successfully", true, Instant.now()));
	}

	public ResponseEntity<ApiResponse<Expense>> updateExpense(String id, UpdateExpenseRequest request) {
		Expense expense = expenseRepo.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Expense not found with ID: " + id));

		if (request.title() != null && !request.title().trim().isEmpty()) {
			expense.setTitle(request.title().trim());
		}
		if (request.amount() != null) {
			expense.setAmount(request.amount());
		}
		if (request.unitPrice() != null) {
			expense.setUnitPrice(request.unitPrice());
		}
		if (request.quantity() != null) {
			expense.setQuantity(request.quantity());
		}
		if (request.notes() != null) {
			expense.setNotes(request.notes().trim());
		}

		Expense saved = expenseRepo.save(expense);
		return ResponseEntity.ok(new ApiResponse<>(saved, "Expense updated successfully ✅", true, Instant.now()));
	}

	public ResponseEntity<ApiResponse<Void>> deleteExpense(String id) {
		Expense expense = expenseRepo.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Expense not found with ID: " + id));

		expenseRepo.delete(expense);
		return ResponseEntity.ok(new ApiResponse<>(null, "Expense deleted successfully", true, Instant.now()));
	}
}
