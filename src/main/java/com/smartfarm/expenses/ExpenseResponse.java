package com.smartfarm.expenses;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseResponse(
		String id,
		String title,
		BigDecimal amount,
		BigDecimal unitPrice,
		BigDecimal quantity,
		LocalDate added_on,
		String notes
		) {
}
