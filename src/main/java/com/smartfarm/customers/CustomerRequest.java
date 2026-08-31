package com.smartfarm.customers;

import jakarta.validation.constraints.NotBlank;

public record CustomerRequest(
		@NotBlank(message = "customer name required!")
		String name,
		@NotBlank(message = "customer contact required!")
		String contact,
		String id_number,
		String address,
		String status
		) {

}
