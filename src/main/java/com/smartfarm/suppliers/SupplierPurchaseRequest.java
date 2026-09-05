package com.smartfarm.suppliers;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SupplierPurchaseRequest(
	String supplierId,
	String inventoryItemId, // optional: if provided, auto-restocks inventory
	BigDecimal restockQuantity, // optional quantity to increment stock
	String invoiceNumber,
	BigDecimal invoiceAmount,
	BigDecimal amountPaid,
	LocalDate purchaseDate,
	LocalDate dueDate,
	String notes
) {}
