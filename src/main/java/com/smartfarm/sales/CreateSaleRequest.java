package com.smartfarm.sales;

import java.math.BigDecimal;

import com.smartfarm.customers.CustomerRequest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateSaleRequest(
		@NotBlank(message = "item name required!")
		String item,
		@NotNull @Positive(message = "input positive quantity value!")
		Float quantity,
		@Positive(message = "input a positive price value!") @NotNull(message = "price per unit required!")
		BigDecimal unit_price,
		@NotNull(message = "project id required!")
		String project_id,
		String customer_id, // existing customer ID
		CustomerRequest customer, // or inline customer creation
		BigDecimal amount_paid, // amount paid at checkout
		String payment_mode // "CASH", "MPESA", "BANK_TRANSFER", "CREDIT_LEDGER"
) {}
