package com.smartfarm.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AdminResetPasswordRequest(
    @NotNull @NotBlank(message = "New password is required")
    String newPassword
) {}
