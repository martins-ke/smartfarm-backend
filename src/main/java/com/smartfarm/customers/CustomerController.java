package com.smartfarm.customers;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartfarm.ApiResponse;

@RestController
@RequestMapping("/customers")
public class CustomerController {

	private final CustomerService customerService;
	
	public CustomerController(CustomerService customerService) {
		this.customerService = customerService;
	}
	
	@GetMapping("/all")
	public ResponseEntity<ApiResponse<List<Customer>>> getAllCustomers() {
		
		return customerService.getAllCustomers();  
	}
}
