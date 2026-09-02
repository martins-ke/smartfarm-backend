package com.smartfarm.user;

public record UpdateProfileRequest(
    String username,
    String currentPassword,
    String newPassword
) {}
