package com.smartfarm.inventory;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateInventoryItemRequest(
    @NotBlank(message = "Item name is required")
    String name,
    @NotBlank(message = "Category is required")
    String category,
    @NotBlank(message = "Unit is required")
    String unit,
    @NotNull(message = "Initial quantity is required")
    @Positive(message = "Quantity must be greater than zero")
    BigDecimal quantityInStock,
    @NotNull(message = "Unit price is required")
    @Positive(message = "Unit price must be greater than zero")
    BigDecimal unitPrice,
    @NotNull(message = "Minimum stock level is required")
    BigDecimal minStockLevel
) {}
