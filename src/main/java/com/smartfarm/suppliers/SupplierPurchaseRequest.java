package com.smartfarm.suppliers;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record SupplierPurchaseRequest(
	@NotBlank(message = "Supplier ID is required")
	String supplierId,

	String inventoryItemId, // optional: if provided, auto-restocks inventory

	@PositiveOrZero(message = "Restock quantity must be positive or zero")
	BigDecimal restockQuantity, // optional quantity to increment stock

	String invoiceNumber, // optional

	@NotNull(message = "Total invoice amount is required")
	@DecimalMin(value = "0.01", message = "Total invoice amount must be greater than zero")
	BigDecimal invoiceAmount,

	@NotNull(message = "Amount paid now is required")
	@PositiveOrZero(message = "Amount paid now cannot be negative")
	BigDecimal amountPaid,

	LocalDate purchaseDate,

	LocalDate dueDate,

	@NotBlank(message = "Delivery notes / items description must be provided for clarity")
	String notes
) {}
