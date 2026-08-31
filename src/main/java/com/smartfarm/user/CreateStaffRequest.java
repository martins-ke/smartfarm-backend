package com.smartfarm.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateStaffRequest(
    @NotNull @NotBlank(message = "Username is required")
    String username,
    @NotNull @NotBlank(message = "Password is required")
    String password,
    @NotNull @NotBlank(message = "Role is required (MANAGER or SUPERVISOR)")
    String role,
    String createdById
) {}
