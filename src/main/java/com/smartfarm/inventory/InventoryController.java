package com.smartfarm.inventory;

import org.springframework.data.domain.Page;
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
import com.smartfarm.expenses.Expense;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/inventory")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
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
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @RequestHeader(value = "X-User-Role", required = false) String headerUserRole,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String userRole) {
        String effectiveUserId = userId != null ? userId : headerUserId;
        String effectiveUserRole = userRole != null ? userRole : headerUserRole;
        return inventoryService.createItem(request, effectiveUserId, effectiveUserRole);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<InventoryItem>> updateItem(
            @PathVariable("id") String id, 
            @Valid @RequestBody CreateInventoryItemRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @RequestHeader(value = "X-User-Role", required = false) String headerUserRole,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String userRole) {
        String effectiveUserId = userId != null ? userId : headerUserId;
        String effectiveUserRole = userRole != null ? userRole : headerUserRole;
        return inventoryService.updateItem(id, request, effectiveUserId, effectiveUserRole);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteItem(
            @PathVariable("id") String id,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @RequestHeader(value = "X-User-Role", required = false) String headerUserRole,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String userRole) {
        String effectiveUserId = userId != null ? userId : headerUserId;
        String effectiveUserRole = userRole != null ? userRole : headerUserRole;
        return inventoryService.deleteItem(id, effectiveUserId, effectiveUserRole);
    }

    @PostMapping("/{id}/use")
    public ResponseEntity<ApiResponse<Expense>> useItem(
            @PathVariable("id") String id, 
            @Valid @RequestBody UseInventoryRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @RequestHeader(value = "X-User-Role", required = false) String headerUserRole,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String userRole) {
        String effectiveUserId = userId != null ? userId : headerUserId;
        String effectiveUserRole = userRole != null ? userRole : headerUserRole;
        return inventoryService.useItem(id, request, effectiveUserId, effectiveUserRole);
    }
}
