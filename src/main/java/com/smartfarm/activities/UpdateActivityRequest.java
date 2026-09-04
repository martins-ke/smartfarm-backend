package com.smartfarm.activities;

import jakarta.validation.constraints.NotBlank;

public record UpdateActivityRequest(
		@NotBlank(message = "Activity title required!") 
		String title,
		@NotBlank(message = "Activity type required!") 
		String type,
		String notes
) {}