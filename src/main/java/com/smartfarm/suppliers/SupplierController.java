package com.smartfarm.suppliers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartfarm.ApiResponse;

@RestController
@RequestMapping("/suppliers")
public class SupplierController {

	private final SupplierService supplierService;

	public SupplierController(SupplierService supplierService) {
		this.supplierService = supplierService;
	}

	@GetMapping
	public ResponseEntity<ApiResponse<List<Supplier>>> getAllSuppliers() {
		return supplierService.getAllSuppliers();
	}

	@GetMapping("/{id}")
	public ResponseEntity<ApiResponse<Supplier>> getSupplierById(@PathVariable String id) {
		return supplierService.getSupplierById(id);
	}

	@PostMapping
	public ResponseEntity<ApiResponse<?>> createSupplier(@RequestBody CreateSupplierRequest request) {
		return supplierService.createSupplier(request);
	}

	@GetMapping("/{id}/purchases")
	public ResponseEntity<ApiResponse<List<SupplierPurchase>>> getSupplierPurchases(@PathVariable String id) {
		return supplierService.getSupplierPurchases(id);
	}

	@PostMapping("/purchases")
	public ResponseEntity<ApiResponse<?>> recordPurchase(@RequestBody SupplierPurchaseRequest request) {
		return supplierService.recordPurchase(request);
	}

	@PostMapping("/{id}/payments")
	public ResponseEntity<ApiResponse<?>> recordPayment(@PathVariable String id, @RequestBody SupplierPaymentRequest request) {
		return supplierService.recordPayment(id, request);
	}
}
