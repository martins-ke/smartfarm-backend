package com.smartfarm.activities;

import java.time.LocalDate;

public record AssignLaborRequest(
	String employeeId,
	LocalDate assignmentDate,
	double hoursWorked,
	String notes
) {}
