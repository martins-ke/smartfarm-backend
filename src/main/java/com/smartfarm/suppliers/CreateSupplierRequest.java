package com.smartfarm.suppliers;

import jakarta.validation.constraints.NotBlank;

public record CreateSupplierRequest(
	@NotBlank(message = "Supplier name is required")
	String name,
	String contactPerson,
	String phoneNumber,
	String email,
	String idOrTaxNumber,
	String category,
	String address
) {}
