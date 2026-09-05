package com.smartfarm.suppliers;

public record CreateSupplierRequest(
	String name,
	String contactPerson,
	String phoneNumber,
	String email,
	String idOrTaxNumber,
	String category,
	String address
) {}
