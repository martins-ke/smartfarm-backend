package com.smartfarm.customers;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CustomerPaymentRequest(
	@NotNull(message = "Payment amount is required")
	@Positive(message = "Payment amount must be greater than zero")
	BigDecimal amount,
	String paymentMode, // "CASH", "MPESA", "BANK_TRANSFER"
	String referenceNumber,
	String saleId, // Optional: specific sale invoice
	String notes,
	LocalDate paymentDate
) {}
