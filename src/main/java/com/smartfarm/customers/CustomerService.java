package com.smartfarm.customers;
import java.util.List;

import java.time.Instant;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import com.smartfarm.ApiResponse;
import com.smartfarm.util.IdGenarator;

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
		
		Customer customer = new Customer(id, request.name().trim(), request.contact().trim(), request.id_number(), address, true);
		return customerRepo.save(customer); 
	}
	
	public ResponseEntity<ApiResponse<List<Customer>>> getAllCustomers() {
		return ResponseEntity.status(200).body(new ApiResponse<>(customerRepo.findAll(), "Customer list fetched successfully ✅", true, Instant.now()));
	}
}
