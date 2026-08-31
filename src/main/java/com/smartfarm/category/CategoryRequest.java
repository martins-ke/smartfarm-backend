package com.smartfarm.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CategoryRequest(
		@NotNull @NotBlank(message = "Category name required!")
		String name, 
		@NotNull @NotBlank(message = "Please add a brief description for clarity!")
		String description
		) {} 
