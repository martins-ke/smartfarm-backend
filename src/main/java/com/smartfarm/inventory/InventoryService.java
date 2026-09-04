package com.smartfarm.inventory;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import com.smartfarm.ApiResponse;
import com.smartfarm.expenses.Expense;
import com.smartfarm.expenses.ExpenseRepository;
import com.smartfarm.projects.Project;
import com.smartfarm.projects.ProjectRepository;
import com.smartfarm.util.IdGenarator;

import com.smartfarm.user.User;
import com.smartfarm.user.UserRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class InventoryService {

    private final InventoryItemRepository inventoryRepo;
    private final ExpenseRepository expenseRepo;
    private final ProjectRepository projectRepo;
    private final UserRepository userRepo;

    public InventoryService(InventoryItemRepository inventoryRepo, ExpenseRepository expenseRepo, ProjectRepository projectRepo, UserRepository userRepo) {
        this.inventoryRepo = inventoryRepo;
        this.expenseRepo = expenseRepo;
        this.projectRepo = projectRepo;
        this.userRepo = userRepo;
    }

    public ResponseEntity<ApiResponse<Page<InventoryItem>>> getAllItems(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        Page<InventoryItem> result = inventoryRepo.findAll(pageable);
        return ResponseEntity.ok(new ApiResponse<>(result, "Inventory retrieved", true, Instant.now()));
    }

    public ResponseEntity<ApiResponse<InventoryItem>> createItem(CreateInventoryItemRequest request) {
        long count = inventoryRepo.count();
        String id = IdGenarator.generateId(request.name(), count);
        while (inventoryRepo.existsById(id)) {
            count++;
            id = IdGenarator.generateId(request.name(), count);
        }
        
        InventoryItem item = new InventoryItem(
            id, request.name(), request.category(), request.unit(), 
            request.quantityInStock(), request.unitPrice(), request.minStockLevel()
        );
        
        return ResponseEntity.status(201).body(new ApiResponse<>(inventoryRepo.save(item), "Inventory item added", true, Instant.now()));
    }

    public ResponseEntity<ApiResponse<InventoryItem>> updateItem(String id, CreateInventoryItemRequest request) {
        InventoryItem item = inventoryRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Inventory item not found!"));
        
        item.setName(request.name());
        item.setCategory(request.category());
        item.setUnit(request.unit());
        item.setQuantityInStock(request.quantityInStock());
        item.setUnitPrice(request.unitPrice());
        item.setMinStockLevel(request.minStockLevel());
        item.setLastRestocked(LocalDate.now()); // Update restock date on edit
        
        return ResponseEntity.ok(new ApiResponse<>(inventoryRepo.save(item), "Inventory item updated", true, Instant.now()));
    }

    public ResponseEntity<ApiResponse<Void>> deleteItem(String id) {
        return deleteItem(id, null, "ADMIN");
    }

    public ResponseEntity<ApiResponse<Void>> deleteItem(String id, String userId, String userRole) {
        boolean authorized = false;

        if (userRole != null && "ADMIN".equalsIgnoreCase(userRole.trim())) {
            authorized = true;
        } else if (userId != null && !userId.trim().isEmpty()) {
            User user = userRepo.findById(userId.trim()).orElse(null);
            if (user != null) {
                if ("ADMIN".equalsIgnoreCase(user.getRole())) {
                    authorized = true;
                } else if ("MANAGER".equalsIgnoreCase(user.getRole()) 
                        && user.getPrivileges() != null 
                        && user.getPrivileges().contains("CAN_DELETE_INVENTORY")) {
                    authorized = true;
                }
            }
        }

        if (!authorized) {
            return ResponseEntity.status(403).body(new ApiResponse<>(null, "You do not have permission to delete inventory items.", false, Instant.now()));
        }

        InventoryItem item = inventoryRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Inventory item not found"));
        inventoryRepo.delete(item);
        return ResponseEntity.ok(new ApiResponse<>(null, "Inventory item deleted", true, Instant.now()));
    }

    @Transactional
    public ResponseEntity<ApiResponse<Expense>> useItem(String id, UseInventoryRequest request) {
        InventoryItem item = inventoryRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Inventory item not found"));
                
        Project project = projectRepo.findById(request.projectId())
                .orElseThrow(() -> new EntityNotFoundException("Project not found"));
                
        if (item.getQuantityInStock().compareTo(request.quantity()) < 0) {
            throw new IllegalArgumentException("Not enough stock available. Current stock: " + item.getQuantityInStock());
        }

        // Deduct stock
        item.setQuantityInStock(item.getQuantityInStock().subtract(request.quantity()));
        inventoryRepo.save(item);

        // Create Expense
        long expenseCount = expenseRepo.count();
        String expenseId = IdGenarator.generateId("Used: " + item.getName(), expenseCount);
        
        BigDecimal totalAmount = item.getUnitPrice().multiply(request.quantity());
        
        Expense expense = new Expense(
            expenseId, 
            "Used: " + item.getName(), 
            totalAmount, 
            item.getUnitPrice(), 
            request.quantity(), 
            LocalDate.now(), 
            request.notes(), 
            project
        );
        
        Expense savedExpense = expenseRepo.save(expense);
        
        return ResponseEntity.ok(new ApiResponse<>(savedExpense, "Supplies used and expense recorded !", true, Instant.now()));
    }
}
