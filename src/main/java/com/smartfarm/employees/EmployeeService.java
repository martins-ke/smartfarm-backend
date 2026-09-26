package com.smartfarm.employees;

import java.time.Instant;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartfarm.ApiResponse;

@Service
public class EmployeeService {

	private final EmployeeRepository employeeRepo;

	public EmployeeService(EmployeeRepository employeeRepo) {
		this.employeeRepo = employeeRepo;
	}

	public ResponseEntity<ApiResponse<List<Employee>>> getAllEmployees() {
		List<Employee> list = employeeRepo.findByIsDeletedFalse();
		return ResponseEntity.ok(new ApiResponse<>(list, "Employees retrieved successfully ✅", true, Instant.now()));
	}

	public ResponseEntity<ApiResponse<List<Employee>>> getActiveEmployees() {
		List<Employee> list = employeeRepo.findByStatusIgnoreCaseAndIsDeletedFalse("ACTIVE");
		return ResponseEntity.ok(new ApiResponse<>(list, "Active employees retrieved successfully ✅", true, Instant.now()));
	}

	@Transactional
	public ResponseEntity<ApiResponse<Employee>> registerEmployee(EmployeeRequest req) {
		if (req.fullName() == null || req.fullName().trim().isEmpty()) {
			return ResponseEntity.status(400).body(new ApiResponse<>(null, "Worker full name is required!", false, Instant.now()));
		}
		if (req.idNumber() == null || req.idNumber().trim().isEmpty()) {
			return ResponseEntity.status(400).body(new ApiResponse<>(null, "Government National ID is required for legal adult age verification!", false, Instant.now()));
		}

		String cleanId = req.idNumber().trim();
		if (!isValidKenyanId(cleanId)) {
			return ResponseEntity.status(400).body(new ApiResponse<>(null, "Invalid National ID format. Must be a valid 7 to 8-digit government adult ID number.", false, Instant.now()));
		}

		if (employeeRepo.existsByIdNumberAndIsDeletedFalse(cleanId)) {
			return ResponseEntity.status(400).body(new ApiResponse<>(null, "An active employee with this National ID is already registered!", false, Instant.now()));
		}

		long count = employeeRepo.count();
		String empId = "EMP-" + String.format("%03d", count + 1);
		while (employeeRepo.existsById(empId)) {
			count++;
			empId = "EMP-" + String.format("%03d", count + 1);
		}

		Employee employee = new Employee(
			empId,
			req.fullName().trim(),
			cleanId,
			req.phoneNumber() != null ? req.phoneNumber().trim() : "",
			req.employmentType() != null ? req.employmentType().trim() : "CASUAL",
			req.dailyRate(),
			"ACTIVE",
			req.registeredById()
		);

		Employee saved = employeeRepo.save(employee);
		return ResponseEntity.status(201).body(new ApiResponse<>(saved, "Employee registered & adult identity verified successfully ✅", true, Instant.now()));
	}

	@Transactional
	public ResponseEntity<ApiResponse<Void>> deleteEmployee(String id) {
		Employee employee = employeeRepo.findById(id).orElse(null);
		if (employee == null || employee.isDeleted()) {
			return ResponseEntity.status(404).body(new ApiResponse<>(null, "Employee not found with ID: " + id, false, Instant.now()));
		}

		// Soft delete: flag as deleted and set inactive, preserving all foreign keys and transaction history
		employee.setDeleted(true);
		employee.setStatus("DELETED");
		employeeRepo.save(employee);

		return ResponseEntity.ok(new ApiResponse<>(null, "Worker deleted successfully (records preserved) ✅", true, Instant.now()));
	}

	@Transactional
	public ResponseEntity<ApiResponse<Employee>> toggleEmployeeStatus(String id, String status) {
		return employeeRepo.findById(id)
				.filter(emp -> !emp.isDeleted())
				.map(emp -> {
					String newStatus = (status != null && !status.trim().isEmpty())
							? status.trim().toUpperCase()
							: (emp.getStatus().equalsIgnoreCase("ACTIVE") ? "INACTIVE" : "ACTIVE");
					emp.setStatus(newStatus);
					Employee saved = employeeRepo.save(emp);
					return ResponseEntity.ok(new ApiResponse<>(saved, "Employee status updated to " + newStatus + " ✅", true, Instant.now()));
				})
				.orElseGet(() -> ResponseEntity.status(404).body(new ApiResponse<>(null, "Active employee not found with ID: " + id, false, Instant.now())));
	}

	@Transactional
	public ResponseEntity<ApiResponse<Employee>> updateEmployee(String id, EmployeeRequest req) {
		Employee employee = employeeRepo.findById(id)
				.filter(emp -> !emp.isDeleted())
				.orElse(null);

		if (employee == null) {
			return ResponseEntity.status(404).body(new ApiResponse<>(null, "Employee not found with ID: " + id, false, Instant.now()));
		}

		if (req.fullName() != null && !req.fullName().trim().isEmpty()) {
			employee.setFullName(req.fullName().trim());
		}

		if (req.idNumber() != null && !req.idNumber().trim().isEmpty()) {
			String cleanId = req.idNumber().trim();
			if (!isValidKenyanId(cleanId)) {
				return ResponseEntity.status(400).body(new ApiResponse<>(null, "Invalid National ID format. Must be a valid 7 to 8-digit government adult ID number.", false, Instant.now()));
			}
			if (!cleanId.equalsIgnoreCase(employee.getIdNumber()) && employeeRepo.existsByIdNumberAndIsDeletedFalse(cleanId)) {
				return ResponseEntity.status(400).body(new ApiResponse<>(null, "An active employee with this National ID is already registered!", false, Instant.now()));
			}
			employee.setIdNumber(cleanId);
		}

		if (req.phoneNumber() != null) {
			employee.setPhoneNumber(req.phoneNumber().trim());
		}

		if (req.employmentType() != null && !req.employmentType().trim().isEmpty()) {
			employee.setEmploymentType(req.employmentType().trim().toUpperCase());
		}

		if (req.dailyRate() != null) {
			employee.setDailyRate(req.dailyRate());
		}

		Employee saved = employeeRepo.save(employee);
		return ResponseEntity.ok(new ApiResponse<>(saved, "Employee details updated successfully ✅", true, Instant.now()));
	}

	private boolean isValidKenyanId(String idNumber) {
		return idNumber != null && idNumber.matches("^\\d{7,8}$");
	}
}
