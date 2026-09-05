package com.smartfarm.employees;

import java.math.BigDecimal;

public record EmployeeRequest(
	String fullName,
	String idNumber, // Kenyan National ID (7-8 digits)
	String phoneNumber,
	String employmentType, // "CASUAL", "PERMANENT"
	BigDecimal dailyRate,
	String registeredById
) {}
