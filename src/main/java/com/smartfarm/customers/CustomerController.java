package com.smartfarm.customers;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
	public ResponseEntity<ApiResponse<List<Customer>>> getAllCustomers() {
		return customerService.getAllCustomers();  
	}

	@GetMapping("/{id}")
	public ResponseEntity<ApiResponse<Customer>> getCustomerById(@PathVariable String id) {
		return customerService.getCustomerById(id);
	}

	@GetMapping("/all")
	public ResponseEntity<ApiResponse<List<Customer>>> getAllCustomersOld() {
		return customerService.getAllCustomers();  
	}

	@PostMapping
	public ResponseEntity<ApiResponse<Customer>> createCustomer(@Valid @RequestBody CustomerRequest request) {
		Customer created = customerService.saveCustomer(request);
		return ResponseEntity.status(201).body(new ApiResponse<>(created, "Customer created successfully ✅", true, Instant.now()));
	}

	@GetMapping("/{id}/payments")
	public ResponseEntity<ApiResponse<List<CustomerPayment>>> getCustomerPayments(@PathVariable String id) {
		return customerService.getCustomerPayments(id);
	}

	@GetMapping("/sales/{saleId}/payments")
	public ResponseEntity<ApiResponse<List<CustomerPayment>>> getSalePaymentsDirect(@PathVariable String saleId) {
		return customerService.getSalePayments(saleId);
	}

	@GetMapping("/{id}/sales/{saleId}/payments")
	public ResponseEntity<ApiResponse<List<CustomerPayment>>> getSalePayments(@PathVariable String id, @PathVariable String saleId) {
		return customerService.getSalePayments(saleId);
	}

	@PostMapping("/{id}/payments")
	public ResponseEntity<ApiResponse<?>> recordPayment(@PathVariable String id, @RequestBody Map<String, Object> body) {
		Object amtObj = body != null ? body.get("amount") : null;
		BigDecimal amount = BigDecimal.ZERO;
		if (amtObj != null && !amtObj.toString().trim().isEmpty()) {
			amount = new BigDecimal(amtObj.toString().trim());
		}

		String paymentMode = body != null && body.get("paymentMode") != null ? body.get("paymentMode").toString() : "CASH";
		String referenceNumber = body != null && body.get("referenceNumber") != null ? body.get("referenceNumber").toString() : null;
		String saleId = body != null && body.get("saleId") != null && !body.get("saleId").toString().trim().isEmpty() 
				? body.get("saleId").toString().trim() 
				: null;
		String notes = body != null && body.get("notes") != null ? body.get("notes").toString() : null;
		
		LocalDate paymentDate = LocalDate.now();
		if (body != null && body.get("paymentDate") != null && !body.get("paymentDate").toString().trim().isEmpty()) {
			try {
				paymentDate = LocalDate.parse(body.get("paymentDate").toString().trim());
			} catch (Exception e) {}
		}

		CustomerPaymentRequest req = new CustomerPaymentRequest(amount, paymentMode, referenceNumber, saleId, notes, paymentDate);
		return customerService.settleCustomerDebt(id, req);
	}
}
