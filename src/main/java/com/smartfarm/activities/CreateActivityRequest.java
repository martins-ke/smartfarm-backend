package com.smartfarm.activities;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateActivityRequest(
		@NotBlank(message = "activity title required!") 
		String title,
		@NotBlank(message = "activity type required!") 
		String type,
		String notes,
		@NotNull(message = "project id missing!")
		String project_id,
		LocalDate scheduledDate,
		LocalDate dueDate,
		String status,
		String priority
) {}
