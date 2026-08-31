package com.smartfarm.sales;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import com.smartfarm.ApiResponse;
import com.smartfarm.customers.Customer;
import com.smartfarm.customers.CustomerRepository;
import com.smartfarm.customers.CustomerService;
import com.smartfarm.projects.Project;
import com.smartfarm.projects.ProjectRepository;
import com.smartfarm.util.IdGenarator;

import com.smartfarm.harvest.HarvestRepository;
import jakarta.persistence.EntityNotFoundException;

@Service
public class SalesService { 

	private final SalesRepository salesRepo;
	private final ProjectRepository projectRepo;
	private final CustomerRepository customerRepo;
	private final CustomerService customerService;
	private final HarvestRepository harvestRepo;
	
	public SalesService(SalesRepository salesRepo, ProjectRepository projectRepo, CustomerRepository customerRepo, CustomerService customerService, HarvestRepository harvestRepo) {
		this.salesRepo = salesRepo;
		this.projectRepo = projectRepo;
		this.customerRepo = customerRepo;
		this.customerService = customerService;
		this.harvestRepo = harvestRepo;
	}
	
	@Transactional
	public ResponseEntity<ApiResponse<Sale>> createSale(CreateSaleRequest request) {
		Project project = projectRepo.findById(request.project_id())
				.orElseThrow(() -> new EntityNotFoundException("Project not in the system!"));
		
		// Enforce business rule: A project MUST have enough harvested quantity to cover the sale
		float totalHarvested = harvestRepo.totalHarvestQuantityByProjectId(project.getId());
		float totalSold = salesRepo.totalSoldQuantityByProjectId(project.getId());
		float quantityRequested = request.quantity();
		
		if (totalHarvested < (totalSold + quantityRequested)) {
			float available = Math.max(0, totalHarvested - totalSold);
			return ResponseEntity.status(400).body(new ApiResponse<>(null, 
				"Sale failed! You cannot sell more than what you have harvested. Only " + available + " available in stock.", false, Instant.now()));
		}

		long count = salesRepo.count();
		String id = IdGenarator.generateId(request.item(), count);
		while (salesRepo.existsById(id)) {
			count++;
			id = IdGenarator.generateId(request.item(), count);
		}
		
		BigDecimal quantity = BigDecimal.valueOf(request.quantity());
		BigDecimal total_amount = request.unit_price().multiply(quantity);
		Customer customer = null;
		
		if (request.customer() != null && request.customer().name() != null && !request.customer().name().trim().isEmpty()) {
			String status = request.customer().status() != null ? request.customer().status().trim().toLowerCase() : "new";
			String contact = request.customer().contact() != null ? request.customer().contact().trim() : null;
			String idNumber = request.customer().id_number() != null ? request.customer().id_number().trim() : null;

			if ("new".equals(status)) {
				if (contact != null && !contact.isEmpty() && customerRepo.existsByContact(contact)) {
					return ResponseEntity.status(400).body(new ApiResponse<>(null, "Failed to save! \nCustomer with contact " + contact + " exists!", false, Instant.now()));
				}
				if (idNumber != null && !idNumber.isEmpty() && customerRepo.existsByIdNumber(idNumber)) {
					return ResponseEntity.status(400).body(new ApiResponse<>(null, "Failed to save! \nCustomer with id number " + idNumber + " exists!", false, Instant.now()));
				}
				customer = customerService.saveCustomer(request.customer());
			} else {
				if (contact != null && !contact.isEmpty()) {
					customer = customerRepo.findByContact(contact).orElse(null);
				}
				if (customer == null && idNumber != null && !idNumber.isEmpty()) {
					customer = customerRepo.findByIdNumber(idNumber).orElse(null);
				}
				if (customer == null) {
					customer = customerService.saveCustomer(request.customer());
				}
			}
		}
		
		Sale sale = new Sale(id, request.item(), request.quantity(), request.unit_price(), LocalDate.now(), total_amount, project, customer);
		return ResponseEntity.status(201).body(new ApiResponse<>(salesRepo.save(sale), "Sale recorded successfully ✅", true, Instant.now()));  
	}

	public ResponseEntity<ApiResponse<Page<Sale>>> getAllSales(int page, int size) {
		Pageable pageable = PageRequest.of(page, size, Sort.by("added_on").descending());
		return ResponseEntity.status(200).body(new ApiResponse<>(salesRepo.findAll(pageable), "Sales fetched successfully", true, Instant.now()));
	}

	public ResponseEntity<ApiResponse<Page<Sale>>> getSalesByProjectId(String projectId, int page, int size) {
		Pageable pageable = PageRequest.of(page, size, Sort.by("added_on").descending());
		return ResponseEntity.status(200).body(new ApiResponse<>(salesRepo.findByProjectId(projectId, pageable), "Sales retrieved successfully", true, Instant.now()));
	}

	public ResponseEntity<ApiResponse<Sale>> getSaleById(String id) {
		Sale sale = salesRepo.findById(id).orElseThrow(() -> new EntityNotFoundException("Sale not found with id: " + id));
		return ResponseEntity.status(200).body(new ApiResponse<>(sale, "Sale details fetched successfully", true, Instant.now()));
	}

	@Transactional
	public ResponseEntity<ApiResponse<Void>> deleteSale(String id) {
		if (!salesRepo.existsById(id)) {
			throw new EntityNotFoundException("Sale not found with id: " + id);
		}
		salesRepo.deleteById(id);
		return ResponseEntity.status(200).body(new ApiResponse<>(null, "Sale deleted successfully", true, Instant.now()));
	}
}
