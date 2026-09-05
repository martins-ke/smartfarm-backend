package com.smartfarm.customers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartfarm.ApiResponse;
import com.smartfarm.util.IdGenarator;

import jakarta.persistence.EntityNotFoundException;

@Service
public class CustomerService {

	private final CustomerRepository customerRepo;
	
	public CustomerService(CustomerRepository customerRepo) {
		this.customerRepo = customerRepo;
	}
	
	@Transactional
	public Customer saveCustomer(CustomerRequest request) { 
		long count = customerRepo.count();
		String id = IdGenarator.generateId(request.name(), count);
		while (customerRepo.existsById(id)) {
			count++;
			id = IdGenarator.generateId(request.name(), count);
		}
		
		String address = (request.address() != null && !request.address().trim().isEmpty())
				? request.address().trim()
				: "-";

		BigDecimal creditLimit = request.credit_limit() != null ? request.credit_limit() : BigDecimal.ZERO;
		
		Customer customer = new Customer(id, request.name().trim(), request.contact().trim(), request.id_number(), address, true, creditLimit);
		if (request.category() != null && !request.category().trim().isEmpty()) {
			customer.setCategory(request.category().trim());
		}
		return customerRepo.save(customer); 
	}
	
	public ResponseEntity<ApiResponse<List<Customer>>> getAllCustomers() {
		return ResponseEntity.status(200).body(new ApiResponse<>(customerRepo.findAll(), "Customer list fetched successfully ✅", true, Instant.now()));
	}

	public ResponseEntity<ApiResponse<Customer>> getCustomerById(String id) {
		Customer customer = customerRepo.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Customer not found with ID: " + id));
		return ResponseEntity.ok(new ApiResponse<>(customer, "Customer retrieved successfully ✅", true, Instant.now()));
	}

	@Transactional
	public ResponseEntity<ApiResponse<?>> settleCustomerDebt(String customerId, BigDecimal paymentAmount) {
		Customer customer = customerRepo.findById(customerId)
				.orElseThrow(() -> new EntityNotFoundException("Customer not found with ID: " + customerId));

		if (paymentAmount == null || paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
			return ResponseEntity.status(400).body(new ApiResponse<>(null, "Payment amount must be greater than zero!", false, Instant.now()));
		}

		customer.setTotalPaid(customer.getTotalPaid().add(paymentAmount));
		BigDecimal remainingDebt = customer.getTotalPurchases().subtract(customer.getTotalPaid());
		if (remainingDebt.compareTo(BigDecimal.ZERO) < 0) {
			remainingDebt = BigDecimal.ZERO;
		}
		customer.setOutstandingDebt(remainingDebt);

		if (remainingDebt.compareTo(BigDecimal.ZERO) == 0) {
			customer.setCreditStatus("CLEAR");
		} else if (customer.getCreditLimit().compareTo(BigDecimal.ZERO) > 0 && remainingDebt.compareTo(customer.getCreditLimit()) > 0) {
			customer.setCreditStatus("BLOCKED");
		} else {
			customer.setCreditStatus("HAS_DEBT");
		}

		Customer updated = customerRepo.save(customer);
		return ResponseEntity.ok(new ApiResponse<>(updated, "Payment of KES " + paymentAmount + " recorded. Remaining debt: KES " + remainingDebt + " ✅", true, Instant.now()));
	}
}
