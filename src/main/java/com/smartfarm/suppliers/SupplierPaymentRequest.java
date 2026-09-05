package com.smartfarm.suppliers;

import java.math.BigDecimal;

public record SupplierPaymentRequest(
	BigDecimal amount,
	String paymentMode, // "CASH", "MPESA", "BANK_TRANSFER"
	String referenceNumber,
	String notes
) {}
