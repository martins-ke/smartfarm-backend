package com.smartfarm.harvest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateHarvestRequest(
		@NotNull @NotBlank(message = "item name required!")
		String item,
		@NotNull(message = "harvest quantity required")
		Float quantity,
		@NotNull @NotBlank(message = "units required!")
		String units,
		String notes,
		@NotNull(message = "project id missing!")
		String project_id
		) {

}
