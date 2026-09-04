package com.smartfarm.expenses;

import java.math.BigDecimal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateExpenseRequest(
		@NotNull @NotBlank(message = "Expense title required!")
		String title, 
		@NotNull
		BigDecimal amount,
		BigDecimal unitPrice,
		BigDecimal quantity,
		String notes
) {}