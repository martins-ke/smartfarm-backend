package com.smartfarm.harvest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateHarvestRequest(
        @NotBlank(message = "Item name is required")
        String item,
        @NotNull(message = "Harvest quantity is required")
        @Positive(message = "Quantity must be greater than zero")
        Float quantity,
        @NotBlank(message = "Units are required")
        String units,
        String notes,
        @NotBlank(message = "Project ID is required")
        String project_id,
        String base_unit,
        String display_unit
) {
}
