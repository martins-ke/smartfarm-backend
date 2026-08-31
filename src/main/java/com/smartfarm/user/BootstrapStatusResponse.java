package com.smartfarm.user;

public record BootstrapStatusResponse(
    boolean isBootstrap,
    long totalUsers,
    long adminCount,
    long managerCount,
    long supervisorCount,
    long maxManagers,
    long maxSupervisors
) {}
