package com.smartfarm.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LoginRequest(
		@NotNull @NotBlank(message = "username required!")
		String username,
		@NotNull @NotBlank(message = "password required!")
		String password
		) {}

