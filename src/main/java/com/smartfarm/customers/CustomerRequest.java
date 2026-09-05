package com.smartfarm.customers;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;

public record CustomerRequest(
		@NotBlank(message = "customer name required!")
		String name,
		@NotBlank(message = "customer contact required!")
		String contact,
		String id_number,
		String address,
		String status,
		BigDecimal credit_limit,
		String category
) {}
