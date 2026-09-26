package com.smartfarm.inventory;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smartfarm.ApiResponse;
import com.smartfarm.expenses.Expense;
import com.smartfarm.user.User;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<InventoryItem>>> getAllItems(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return inventoryService.getAllItems(page, size);
    }

    @PostMapping
    public ResponseEntity<ApiResponse<InventoryItem>> createItem(
            @Valid @RequestBody CreateInventoryItemRequest request,
            @AuthenticationPrincipal User currentUser) {
        return inventoryService.createItem(request, currentUser);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<InventoryItem>> updateItem(
            @PathVariable("id") String id, 
            @Valid @RequestBody CreateInventoryItemRequest request,
            @AuthenticationPrincipal User currentUser) {
        return inventoryService.updateItem(id, request, currentUser);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteItem(
            @PathVariable("id") String id,
            @AuthenticationPrincipal User currentUser) {
        return inventoryService.deleteItem(id, currentUser);
    }

    @PostMapping("/{id}/use")
    public ResponseEntity<ApiResponse<Expense>> useItem(
            @PathVariable("id") String id, 
            @Valid @RequestBody UseInventoryRequest request,
            @AuthenticationPrincipal User currentUser) {
        return inventoryService.useItem(id, request, currentUser);
    }
}
