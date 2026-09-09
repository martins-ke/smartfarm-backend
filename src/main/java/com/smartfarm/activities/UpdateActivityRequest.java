package com.smartfarm.activities;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;

public record UpdateActivityRequest(
		@NotBlank(message = "Activity title required!") 
		String title,
		@NotBlank(message = "Activity type required!") 
		String type,
		String notes,
		LocalDate scheduledDate,
		LocalDate dueDate,
		String status,
		String priority
) {}