package com.smartfarm.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.smartfarm.ApiResponse;
import com.smartfarm.expenses.Expense;
import com.smartfarm.expenses.ExpenseRepository;
import com.smartfarm.projects.Project;
import com.smartfarm.projects.ProjectRepository;
import com.smartfarm.user.User;
import com.smartfarm.user.UserRepository;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryItemRepository inventoryRepo;

    @Mock
    private ExpenseRepository expenseRepo;

    @Mock
    private ProjectRepository projectRepo;

    @Mock
    private UserRepository userRepo;

    @InjectMocks
    private InventoryService inventoryService;

    private InventoryItem sampleItem;

    @BeforeEach
    void setUp() {
        sampleItem = new InventoryItem(
            "INV001", "DAP Fertilizer", "Fertilizer", "Bags",
            new BigDecimal("50.0"), new BigDecimal("3500.00"), new BigDecimal("10.0")
        );
    }

    @Test
    void deleteItem_asAdminRole_success() {
        when(inventoryRepo.findById("INV001")).thenReturn(Optional.of(sampleItem));

        ResponseEntity<ApiResponse<Void>> response = inventoryService.deleteItem("INV001", null, "ADMIN");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().success());
        assertEquals("Inventory item deleted", response.getBody().message());
        verify(inventoryRepo).delete(sampleItem);
    }

    @Test
    void deleteItem_asAdminUserById_success() {
        User adminUser = new User("U_ADMIN", "admin", "admin@smartfarm.com", "pass", "ADMIN", "ACTIVE", null);
        when(userRepo.findById("U_ADMIN")).thenReturn(Optional.of(adminUser));
        when(inventoryRepo.findById("INV001")).thenReturn(Optional.of(sampleItem));

        ResponseEntity<ApiResponse<Void>> response = inventoryService.deleteItem("INV001", "U_ADMIN", null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().success());
        verify(inventoryRepo).delete(sampleItem);
    }

    @Test
    void deleteItem_asManagerWithPrivilege_success() {
        User manager = new User("U_MGR1", "manager1", "mgr@smartfarm.com", "pass", "MANAGER", "ACTIVE", null);
        manager.setPrivileges(new HashSet<>(Set.of("CAN_DELETE_INVENTORY", "CAN_VIEW_FINANCIALS")));
        
        when(userRepo.findById("U_MGR1")).thenReturn(Optional.of(manager));
        when(inventoryRepo.findById("INV001")).thenReturn(Optional.of(sampleItem));

        ResponseEntity<ApiResponse<Void>> response = inventoryService.deleteItem("INV001", "U_MGR1", "MANAGER");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().success());
        verify(inventoryRepo).delete(sampleItem);
    }

    @Test
    void deleteItem_asManagerWithoutPrivilege_returnsForbidden() {
        User manager = new User("U_MGR2", "manager2", "mgr2@smartfarm.com", "pass", "MANAGER", "ACTIVE", null);
        manager.setPrivileges(new HashSet<>(Set.of("CAN_CREATE_CATEGORIES"))); // No CAN_DELETE_INVENTORY

        when(userRepo.findById("U_MGR2")).thenReturn(Optional.of(manager));

        ResponseEntity<ApiResponse<Void>> response = inventoryService.deleteItem("INV001", "U_MGR2", "MANAGER");

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertFalse(response.getBody().success());
        assertEquals("You do not have permission to delete inventory items.", response.getBody().message());
        verify(inventoryRepo, never()).delete(any());
    }

    @Test
    void deleteItem_asSupervisor_returnsForbidden() {
        User supervisor = new User("U_SUP", "sup1", "sup@smartfarm.com", "pass", "SUPERVISOR", "ACTIVE", null);
        when(userRepo.findById("U_SUP")).thenReturn(Optional.of(supervisor));

        ResponseEntity<ApiResponse<Void>> response = inventoryService.deleteItem("INV001", "U_SUP", "SUPERVISOR");

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertFalse(response.getBody().success());
        assertEquals("You do not have permission to delete inventory items.", response.getBody().message());
        verify(inventoryRepo, never()).delete(any());
    }

    @Test
    void deleteItem_withNoAuthContext_returnsForbidden() {
        ResponseEntity<ApiResponse<Void>> response = inventoryService.deleteItem("INV001", null, null);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertFalse(response.getBody().success());
        verify(inventoryRepo, never()).delete(any());
    }

    @Test
    void useItem_deductsStockAndRecordsExpense() {
        Project project = new Project();
        project.setId("P001");
        project.setName("Maize Farm");

        when(inventoryRepo.findById("INV001")).thenReturn(Optional.of(sampleItem));
        when(projectRepo.findById("P001")).thenReturn(Optional.of(project));
        when(expenseRepo.count()).thenReturn(0L);
        when(expenseRepo.save(any(Expense.class))).thenAnswer(i -> i.getArgument(0));

        UseInventoryRequest request = new UseInventoryRequest("P001", new BigDecimal("5.0"), "Planting season");
        ResponseEntity<ApiResponse<Expense>> response = inventoryService.useItem("INV001", request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().success());
        assertEquals(new BigDecimal("45.0"), sampleItem.getQuantityInStock());
        verify(inventoryRepo).save(sampleItem);
        verify(expenseRepo).save(any(Expense.class));
    }

    @Test
    void useItem_insufficientStock_throwsException() {
        Project project = new Project();
        project.setId("P001");

        when(inventoryRepo.findById("INV001")).thenReturn(Optional.of(sampleItem));
        when(projectRepo.findById("P001")).thenReturn(Optional.of(project));

        UseInventoryRequest request = new UseInventoryRequest("P001", new BigDecimal("100.0"), "Too much");
        assertThrows(IllegalArgumentException.class, () -> inventoryService.useItem("INV001", request));
    }
}
