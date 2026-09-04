package com.smartfarm.sales;

import java.math.BigDecimal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record UpdateSaleRequest(
		@NotBlank(message = "Item name required!")
		String item,
		@NotNull @Positive(message = "Quantity must be positive!")
		Float quantity,
		@Positive(message = "Price per unit must be positive!") @NotNull(message = "Price per unit required!")
		BigDecimal unit_price
) {}