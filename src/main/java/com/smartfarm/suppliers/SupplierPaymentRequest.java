package com.smartfarm.suppliers;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record SupplierPaymentRequest(
	@NotNull(message = "Payment amount is required")
	@DecimalMin(value = "0.01", message = "Payment amount must be greater than zero")
	BigDecimal amount,
	String paymentMode, // "CASH", "MPESA", "BANK_TRANSFER"
	String referenceNumber,
	String notes
) {}
