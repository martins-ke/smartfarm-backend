package com.smartfarm.harvest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateHarvestRequest(
		@NotNull @NotBlank(message = "Item name required!")
		String item,
		@NotNull(message = "Harvest quantity required!")
		Float quantity,
		@NotNull @NotBlank(message = "Units required!")
		String units,
		String notes
) {}