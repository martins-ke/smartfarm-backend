package com.smartfarm.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusRequest(
    @NotNull @NotBlank(message = "Status is required (ACTIVE, PENDING_APPROVAL, DISABLED)")
    String status
) {}
