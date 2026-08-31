package com.smartfarm.inventory;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record UseInventoryRequest(
    @NotBlank(message = "Project ID is required")
    String projectId,
    @NotNull(message = "Quantity to use is required")
    @Positive(message = "Quantity must be greater than zero")
    BigDecimal quantity,
    @NotBlank(message = "Notes are required")
    String notes
) {}
