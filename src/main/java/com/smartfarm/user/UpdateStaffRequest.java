package com.smartfarm.user;

import java.util.Set;

public record UpdateStaffRequest(
	String username,
	String email,
	String role,
	String status,
	Integer maxProjectCapacity,
	Set<String> categoryIds,
	Set<String> privileges
) {}
