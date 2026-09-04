package com.smartfarm.user;

import java.util.Set;

public record UpdatePrivilegesRequest(
    Set<String> privileges,
    Integer maxProjectCapacity
) {}
