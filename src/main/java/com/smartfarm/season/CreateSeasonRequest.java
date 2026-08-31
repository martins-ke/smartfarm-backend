package com.smartfarm.season;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateSeasonRequest(
		@NotNull(message = "Season name required!") @NotBlank(message = "Season name required!")
		String name,
		@Positive(message = "size required !") @NotNull(message = "size required!")
		float size,
		@Positive(message = "estimated period required !") @NotNull(message = "estimated period required!")
		float period,
		@Positive(message = "estimated budget required !") @NotNull(message = "estimated budget required!")
		double budget 
		) {

}
