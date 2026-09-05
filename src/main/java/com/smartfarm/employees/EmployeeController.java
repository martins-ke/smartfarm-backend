package com.smartfarm.employees;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smartfarm.ApiResponse;

@RestController
@RequestMapping("/employees")
public class EmployeeController {

	private final EmployeeService employeeService;

	public EmployeeController(EmployeeService employeeService) {
		this.employeeService = employeeService;
	}

	@GetMapping
	public ResponseEntity<ApiResponse<List<Employee>>> getAllEmployees(@RequestParam(required = false) Boolean activeOnly) {
		if (Boolean.TRUE.equals(activeOnly)) {
			return employeeService.getActiveEmployees();
		}
		return employeeService.getAllEmployees();
	}

	@PostMapping
	public ResponseEntity<ApiResponse<Employee>> registerEmployee(@RequestBody EmployeeRequest request) {
		return employeeService.registerEmployee(request);
	}

	@PatchMapping("/{id}/status")
	public ResponseEntity<ApiResponse<Employee>> toggleStatus(@PathVariable String id, @RequestBody(required = false) Map<String, String> body) {
		String status = (body != null) ? body.get("status") : null;
		return employeeService.toggleEmployeeStatus(id, status);
	}
}
