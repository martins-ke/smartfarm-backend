package com.smartfarm.expenses;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateExpenseRequest(
		@NotNull @NotBlank(message = "Expense title required!")
		String title, 
		@NotNull
		BigDecimal amount,
		BigDecimal unitPrice,
		BigDecimal quantity,
		@NotBlank(message = "add a few notes about record !")
		String notes, 
		@NotBlank(message = "Project id must be included in the request!")
		String project_id
		) {}
