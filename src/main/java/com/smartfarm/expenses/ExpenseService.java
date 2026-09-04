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
	private final com.smartfarm.user.UserRepository userRepo;
	
	public ExpenseService(ExpenseRepository expenseRepo, ProjectRepository projectRepo, com.smartfarm.user.UserRepository userRepo) {
		this.expenseRepo = expenseRepo;
		this.projectRepo = projectRepo;
		this.userRepo = userRepo;
	}
	
	public ResponseEntity<ApiResponse<Expense>> createExpense(CreateExpenseRequest request, String userId, String userRole){
		Project project = projectRepo.findById(request.project_id()).orElseThrow(()-> new EntityNotFoundException("Project not in the system!"));

		if ("SUPERVISOR".equalsIgnoreCase(userRole)) {
			boolean isAssigned = project.getSupervisor() != null && userId != null && userId.trim().equals(project.getSupervisor().getId());
			if (!isAssigned) {
				return ResponseEntity.status(403).body(new ApiResponse<>(null, "Access Denied: You are not assigned to supervise this project.", false, Instant.now()));
			}
			if (userId != null) {
				com.smartfarm.user.User sup = userRepo.findById(userId.trim()).orElse(null);
				if (sup == null || sup.getPrivileges() == null || !sup.getPrivileges().contains("CAN_RECORD_EXPENSES")) {
					return ResponseEntity.status(403).body(new ApiResponse<>(null, "Access Denied: You do not have privilege to record expenses.", false, Instant.now()));
				}
			}
		} else if ("MANAGER".equalsIgnoreCase(userRole) && userId != null && !userId.trim().isEmpty()) {
			com.smartfarm.user.User manager = userRepo.findById(userId.trim()).orElse(null);
			if (manager != null) {
				boolean isAssigned = manager.getAssignedCategories().stream()
						.anyMatch(c -> c.getId().equalsIgnoreCase(project.getCategory().getId()));
				if (!isAssigned) {
					return ResponseEntity.status(403).body(new ApiResponse<>(null, "Access Denied: You are not assigned to manage the category for this project.", false, Instant.now()));
				}
			}
		}

		long count = expenseRepo.count(); 
		String id = IdGenarator.generateId(request.title(), count);
		while (expenseRepo.existsById(id)) {
			count++;
			id = IdGenarator.generateId(request.title(), count);
		} 
		Expense expense = new Expense(id, request.title(), request.amount(), request.unitPrice(), request.quantity(), LocalDate.now(), request.notes(), project);
		
		return ResponseEntity.status(201).body(new ApiResponse<>(expenseRepo.save(expense), "Expense recorded successfully ✅", true, Instant.now())); 
	}

	public ResponseEntity<ApiResponse<Expense>> createExpense(CreateExpenseRequest request) {
		return createExpense(request, null, null);
	}
	
	public ResponseEntity<ApiResponse<List<Expense>>> getExpensesByProjectId(String projectId, String userId, String userRole) {
		if ("SUPERVISOR".equalsIgnoreCase(userRole)) {
			return ResponseEntity.status(200).body(new ApiResponse<>(java.util.Collections.emptyList(), "Expenses shielded for supervisor.", true, Instant.now()));
		}
		if ("MANAGER".equalsIgnoreCase(userRole) && userId != null && !userId.trim().isEmpty()) {
			com.smartfarm.user.User manager = userRepo.findById(userId.trim()).orElse(null);
			if (manager != null && (manager.getPrivileges() == null || !manager.getPrivileges().contains("CAN_VIEW_FINANCIALS"))) {
				return ResponseEntity.status(200).body(new ApiResponse<>(java.util.Collections.emptyList(), "Financial privileges required to view expenses.", true, Instant.now()));
			}
		}
		return ResponseEntity.status(200).body(new ApiResponse<>(expenseRepo.findByProjectId(projectId), "Expense list fetched successfully", true, Instant.now()));
	}

	public ResponseEntity<ApiResponse<List<Expense>>> getExpensesByProjectId(String projectId) {
		return getExpensesByProjectId(projectId, null, null);
	}

	public ResponseEntity<ApiResponse<Expense>> updateExpense(String id, UpdateExpenseRequest request, String userId, String userRole) {
		if ("SUPERVISOR".equalsIgnoreCase(userRole)) {
			return ResponseEntity.status(403).body(new ApiResponse<>(null, "Access Denied: Supervisors cannot edit expenses.", false, Instant.now()));
		}

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

	public ResponseEntity<ApiResponse<Expense>> updateExpense(String id, UpdateExpenseRequest request) {
		return updateExpense(id, request, null, null);
	}

	public ResponseEntity<ApiResponse<Void>> deleteExpense(String id, String userId, String userRole) {
		if ("SUPERVISOR".equalsIgnoreCase(userRole)) {
			return ResponseEntity.status(403).body(new ApiResponse<>(null, "Access Denied: Supervisors cannot delete expenses.", false, Instant.now()));
		}

		Expense expense = expenseRepo.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Expense not found with ID: " + id));

		expenseRepo.delete(expense);
		return ResponseEntity.ok(new ApiResponse<>(null, "Expense deleted successfully", true, Instant.now()));
	}

	public ResponseEntity<ApiResponse<Void>> deleteExpense(String id) {
		return deleteExpense(id, null, null);
	}
}
