package com.smartfarm.sales;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smartfarm.ApiResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/sales")
public class SalesController {

	private final SalesService salesService;
	
	public SalesController(SalesService salesService) {
		this.salesService = salesService;
	}
	
	@PostMapping("/create")
	public ResponseEntity<ApiResponse<Sale>> createSale(@Valid @RequestBody CreateSaleRequest request) {
		return salesService.createSale(request); 
	}

	@GetMapping("/all")
	public ResponseEntity<ApiResponse<Page<Sale>>> getAllSales(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return salesService.getAllSales(page, size);
	}

	@GetMapping("/project/{projectId}")
	public ResponseEntity<ApiResponse<Page<Sale>>> getSalesByProjectId(
			@PathVariable String projectId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		 return salesService.getSalesByProjectId(projectId, page, size);
	}

	@GetMapping("/{id}")
	public ResponseEntity<ApiResponse<Sale>> getSaleById(@PathVariable String id) {
		return salesService.getSaleById(id);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<ApiResponse<Void>> deleteSale(@PathVariable String id) {
		return salesService.deleteSale(id);
	}
}
