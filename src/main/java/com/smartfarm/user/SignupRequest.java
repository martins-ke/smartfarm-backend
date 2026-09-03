package com.smartfarm.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SignupRequest(
		@NotNull @NotBlank(message = "username required !")
		String username, 
		@NotNull @NotBlank(message = "password required !")
		String password, 
		@NotNull @NotBlank(message = "confirm password required !")
		String cpassword,
		String email,
		String role
		) {}
