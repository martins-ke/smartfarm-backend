package com.smartfarm.customers;

import java.time.Instant;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.smartfarm.user.User;
import org.springframework.web.bind.annotation.RestController;

import com.smartfarm.ApiResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/customers")
public class CustomerController {

	private final CustomerService customerService;
	
	public CustomerController(CustomerService customerService) {
		this.customerService = customerService;
	}
	
	@GetMapping
	public ResponseEntity<ApiResponse<List<Customer>>> getAllCustomers(@AuthenticationPrincipal User currentUser) {
		return customerService.getAllCustomers();  
	}

	@GetMapping("/{id}")
	public ResponseEntity<ApiResponse<Customer>> getCustomerById(@PathVariable String id, @AuthenticationPrincipal User currentUser) {
		return customerService.getCustomerById(id);
	}

	@GetMapping("/all")
	public ResponseEntity<ApiResponse<List<Customer>>> getAllCustomersOld(@AuthenticationPrincipal User currentUser) {
		return customerService.getAllCustomers();  
	}

	@PostMapping
	public ResponseEntity<ApiResponse<Customer>> createCustomer(@Valid @RequestBody CustomerRequest request, @AuthenticationPrincipal User currentUser) {
		Customer created = customerService.saveCustomer(request);
		return ResponseEntity.status(201).body(new ApiResponse<>(created, "Customer created successfully ✅", true, Instant.now()));
	}

	@GetMapping("/{id}/payments")
	public ResponseEntity<ApiResponse<List<CustomerPayment>>> getCustomerPayments(@PathVariable String id, @AuthenticationPrincipal User currentUser) {
		return customerService.getCustomerPayments(id);
	}

	@GetMapping("/sales/{saleId}/payments")
	public ResponseEntity<ApiResponse<List<CustomerPayment>>> getSalePaymentsDirect(@PathVariable String saleId, @AuthenticationPrincipal User currentUser) {
		return customerService.getSalePayments(saleId);
	}

	@GetMapping("/{id}/sales/{saleId}/payments")
	public ResponseEntity<ApiResponse<List<CustomerPayment>>> getSalePayments(@PathVariable String id, @PathVariable String saleId, @AuthenticationPrincipal User currentUser) {
		return customerService.getSalePayments(saleId);
	}

	@PostMapping("/{id}/payments")
	public ResponseEntity<ApiResponse<?>> recordPayment(@PathVariable String id, @Valid @RequestBody CustomerPaymentRequest request, @AuthenticationPrincipal User currentUser) {
		return customerService.settleCustomerDebt(id, request);
	}
}
