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
	private final com.smartfarm.user.UserRepository userRepo;
	
	public SalesService(SalesRepository salesRepo, ProjectRepository projectRepo, CustomerRepository customerRepo, CustomerService customerService, HarvestRepository harvestRepo, com.smartfarm.user.UserRepository userRepo) {
		this.salesRepo = salesRepo;
		this.projectRepo = projectRepo;
		this.customerRepo = customerRepo;
		this.customerService = customerService;
		this.harvestRepo = harvestRepo;
		this.userRepo = userRepo;
	}
	
	@Transactional
	public ResponseEntity<ApiResponse<Sale>> createSale(CreateSaleRequest request, String userId, String userRole) {
		Project project = projectRepo.findById(request.project_id())
				.orElseThrow(() -> new EntityNotFoundException("Project not in the system!"));

		if ("SUPERVISOR".equalsIgnoreCase(userRole)) {
			boolean isAssigned = project.getSupervisor() != null && userId != null && userId.trim().equals(project.getSupervisor().getId());
			if (!isAssigned) {
				return ResponseEntity.status(403).body(new ApiResponse<>(null, "Access Denied: You are not assigned to supervise this project.", false, Instant.now()));
			}
			if (userId != null) {
				com.smartfarm.user.User sup = userRepo.findById(userId.trim()).orElse(null);
				if (sup == null || sup.getPrivileges() == null || !sup.getPrivileges().contains("CAN_RECORD_SALES")) {
					return ResponseEntity.status(403).body(new ApiResponse<>(null, "Access Denied: You do not have privilege to record farm-gate sales.", false, Instant.now()));
				}
			}
		} else if ("MANAGER".equalsIgnoreCase(userRole) && userId != null && !userId.trim().isEmpty()) {
			com.smartfarm.user.User manager = userRepo.findById(userId.trim()).orElse(null);
			if (manager != null) {
				boolean isAssigned = manager.getAssignedCategories().stream()
						.anyMatch(c -> c.getId().equalsIgnoreCase(project.getCategory().getId()));
				if (!isAssigned) {
					return ResponseEntity.status(403).body(new ApiResponse<>(null, "Access Denied: You are not assigned to manage the category for this project.", false, Instant.now()));
				}
			}
		}
		
		// Enforce business rule: A project MUST have enough harvested quantity to cover the sale of the specific item
		String requestedItem = request.item() != null ? request.item().trim() : "";
		float totalHarvestedForItem = harvestRepo.totalHarvestQuantityByProjectIdAndItem(project.getId(), requestedItem);
		float totalSoldForItem = salesRepo.totalSoldQuantityByProjectIdAndItem(project.getId(), requestedItem);
		float quantityRequested = request.quantity();

		// If no exact match, check case-insensitive or partial match among project's harvests
		if (totalHarvestedForItem == 0) {
			java.util.List<com.smartfarm.harvest.Harvest> harvests = harvestRepo.findByProjectId(project.getId());
			if (harvests.isEmpty()) {
				return ResponseEntity.status(400).body(new ApiResponse<>(null, 
					"Sale failed! No harvest has been recorded for this project yet. Please record a harvest before selling.", false, Instant.now()));
			}

			// Try to find matching harvest record by name ignoring case or contains
			com.smartfarm.harvest.Harvest matchedHarvest = harvests.stream()
				.filter(h -> h.getItem() != null && (
					h.getItem().trim().equalsIgnoreCase(requestedItem) ||
					h.getItem().toLowerCase().contains(requestedItem.toLowerCase()) ||
					requestedItem.toLowerCase().contains(h.getItem().toLowerCase())
				))
				.findFirst()
				.orElse(null);

			if (matchedHarvest != null) {
				totalHarvestedForItem = harvestRepo.totalHarvestQuantityByProjectIdAndItem(project.getId(), matchedHarvest.getItem());
				totalSoldForItem = salesRepo.totalSoldQuantityByProjectIdAndItem(project.getId(), matchedHarvest.getItem());
			} else if (harvests.size() == 1) {
				// If project has only one harvested crop type, use that
				com.smartfarm.harvest.Harvest singleHarvest = harvests.get(0);
				totalHarvestedForItem = harvestRepo.totalHarvestQuantityByProjectIdAndItem(project.getId(), singleHarvest.getItem());
				totalSoldForItem = salesRepo.totalSoldQuantityByProjectIdAndItem(project.getId(), singleHarvest.getItem());
			} else {
				String availableItems = harvests.stream()
					.map(com.smartfarm.harvest.Harvest::getItem)
					.filter(java.util.Objects::nonNull)
					.distinct()
					.collect(java.util.stream.Collectors.joining(", "));
				return ResponseEntity.status(400).body(new ApiResponse<>(null, 
					"Sale failed! No harvest recorded for '" + requestedItem + "'. Available harvested items in this project: " + availableItems, false, Instant.now()));
			}
		}

		if (totalHarvestedForItem < (totalSoldForItem + quantityRequested)) {
			float available = Math.max(0, totalHarvestedForItem - totalSoldForItem);
			return ResponseEntity.status(400).body(new ApiResponse<>(null, 
				"Sale failed! You cannot sell more than what you have harvested for " + requestedItem + ". Only " + available + " available in stock.", false, Instant.now()));
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
		
		if (request.customer_id() != null && !request.customer_id().trim().isEmpty()) {
			customer = customerRepo.findById(request.customer_id().trim()).orElse(null);
		} else if (request.customer() != null && request.customer().name() != null && !request.customer().name().trim().isEmpty()) {
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
					return ResponseEntity.status(400).body(new ApiResponse<>(null, "Customer with provided contact or ID number not found in existing records!", false, Instant.now()));
				}
			}
		}

		BigDecimal amountPaid = request.amount_paid() != null ? request.amount_paid() : total_amount;
		if (amountPaid.compareTo(BigDecimal.ZERO) < 0) {
			amountPaid = BigDecimal.ZERO;
		}
		if (amountPaid.compareTo(total_amount) > 0) {
			amountPaid = total_amount;
		}

		BigDecimal balanceDue = total_amount.subtract(amountPaid);
		String paymentMode = request.payment_mode() != null ? request.payment_mode().toUpperCase() : "CASH";
		String paymentStatus = "PAID_IN_FULL";
		if (amountPaid.compareTo(BigDecimal.ZERO) == 0) {
			paymentStatus = "CREDIT_UNPAID";
		} else if (balanceDue.compareTo(BigDecimal.ZERO) > 0) {
			paymentStatus = "PARTIAL_PAYMENT";
		}

		if (balanceDue.compareTo(BigDecimal.ZERO) > 0 && customer == null) {
			return ResponseEntity.status(400).body(new ApiResponse<>(null, 
				"Sale failed! When the amount paid (Ksh " + amountPaid + ") is less than the required amount (Ksh " + total_amount + "), a customer must be attached to track the balance due of Ksh " + balanceDue + ".", false, Instant.now()));
		}

		if (customer != null) {
			if ("BLOCKED".equalsIgnoreCase(customer.getCreditStatus()) && balanceDue.compareTo(BigDecimal.ZERO) > 0) {
				return ResponseEntity.status(400).body(new ApiResponse<>(null, "Customer credit is BLOCKED due to exceeding credit limit! Full payment required to proceed.", false, Instant.now()));
			}

			customer.setTotalPurchases(customer.getTotalPurchases().add(total_amount));
			customer.setTotalPaid(customer.getTotalPaid().add(amountPaid));
			BigDecimal newDebt = customer.getTotalPurchases().subtract(customer.getTotalPaid());
			if (newDebt.compareTo(BigDecimal.ZERO) < 0) newDebt = BigDecimal.ZERO;
			customer.setOutstandingDebt(newDebt);

			if (newDebt.compareTo(BigDecimal.ZERO) == 0) {
				customer.setCreditStatus("CLEAR");
			} else if (customer.getCreditLimit().compareTo(BigDecimal.ZERO) > 0 && newDebt.compareTo(customer.getCreditLimit()) > 0) {
				customer.setCreditStatus("BLOCKED");
			} else {
				customer.setCreditStatus("HAS_DEBT");
			}
			customerRepo.save(customer);
		}
		
		Sale sale = new Sale(id, request.item(), request.quantity(), request.unit_price(), LocalDate.now(), total_amount, amountPaid, balanceDue, paymentMode, paymentStatus, project, customer);
		return ResponseEntity.status(201).body(new ApiResponse<>(salesRepo.save(sale), "Sale recorded & customer ledger updated successfully ✅", true, Instant.now()));  
	}

	public ResponseEntity<ApiResponse<Sale>> createSale(CreateSaleRequest request) {
		return createSale(request, null, null);
	}

	public ResponseEntity<ApiResponse<Page<Sale>>> getAllSales(int page, int size, String userId, String userRole) {
		if ("SUPERVISOR".equalsIgnoreCase(userRole)) {
			return ResponseEntity.status(200).body(new ApiResponse<>(org.springframework.data.domain.Page.empty(), "Sales records shielded for supervisor.", true, Instant.now()));
		}
		if ("MANAGER".equalsIgnoreCase(userRole) && userId != null && !userId.trim().isEmpty()) {
			com.smartfarm.user.User manager = userRepo.findById(userId.trim()).orElse(null);
			if (manager != null && (manager.getPrivileges() == null || !manager.getPrivileges().contains("CAN_VIEW_FINANCIALS"))) {
				return ResponseEntity.status(200).body(new ApiResponse<>(org.springframework.data.domain.Page.empty(), "Financial privileges required to view sales.", true, Instant.now()));
			}
		}
		Pageable pageable = PageRequest.of(page, size, Sort.by("added_on").descending());
		return ResponseEntity.status(200).body(new ApiResponse<>(salesRepo.findAll(pageable), "Sales fetched successfully", true, Instant.now()));
	}

	public ResponseEntity<ApiResponse<Page<Sale>>> getAllSales(int page, int size) {
		return getAllSales(page, size, null, null);
	}

	public ResponseEntity<ApiResponse<Page<Sale>>> getSalesByProjectId(String projectId, int page, int size, String userId, String userRole) {
		if ("SUPERVISOR".equalsIgnoreCase(userRole)) {
			return ResponseEntity.status(200).body(new ApiResponse<>(org.springframework.data.domain.Page.empty(), "Sales records shielded for supervisor.", true, Instant.now()));
		}
		if ("MANAGER".equalsIgnoreCase(userRole) && userId != null && !userId.trim().isEmpty()) {
			com.smartfarm.user.User manager = userRepo.findById(userId.trim()).orElse(null);
			if (manager != null && (manager.getPrivileges() == null || !manager.getPrivileges().contains("CAN_VIEW_FINANCIALS"))) {
				return ResponseEntity.status(200).body(new ApiResponse<>(org.springframework.data.domain.Page.empty(), "Financial privileges required to view sales.", true, Instant.now()));
			}
		}
		Pageable pageable = PageRequest.of(page, size, Sort.by("added_on").descending());
		return ResponseEntity.status(200).body(new ApiResponse<>(salesRepo.findByProjectId(projectId, pageable), "Sales retrieved successfully", true, Instant.now()));
	}

	public ResponseEntity<ApiResponse<Page<Sale>>> getSalesByProjectId(String projectId, int page, int size) {
		return getSalesByProjectId(projectId, page, size, null, null);
	}

	public ResponseEntity<ApiResponse<Sale>> getSaleById(String id) {
		Sale sale = salesRepo.findById(id).orElseThrow(() -> new EntityNotFoundException("Sale not found with id: " + id));
		return ResponseEntity.status(200).body(new ApiResponse<>(sale, "Sale details fetched successfully", true, Instant.now()));
	}

	@Transactional
	public ResponseEntity<ApiResponse<Void>> deleteSale(String id, String userId, String userRole) {
		if ("SUPERVISOR".equalsIgnoreCase(userRole)) {
			return ResponseEntity.status(403).body(new ApiResponse<>(null, "Access Denied: Supervisors cannot delete sales.", false, Instant.now()));
		}
		if (!salesRepo.existsById(id)) {
			throw new EntityNotFoundException("Sale not found with id: " + id);
		}
		salesRepo.deleteById(id);
		return ResponseEntity.status(200).body(new ApiResponse<>(null, "Sale deleted successfully", true, Instant.now()));
	}

	public ResponseEntity<ApiResponse<Void>> deleteSale(String id) {
		return deleteSale(id, null, null);
	}

	@Transactional
	public ResponseEntity<ApiResponse<Sale>> updateSale(String id, UpdateSaleRequest request, String userId, String userRole) {
		if ("SUPERVISOR".equalsIgnoreCase(userRole)) {
			return ResponseEntity.status(403).body(new ApiResponse<>(null, "Access Denied: Supervisors cannot edit sales records.", false, Instant.now()));
		}

		Sale sale = salesRepo.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Sale not found with id: " + id));

		if (request.item() != null && !request.item().trim().isEmpty()) {
			sale.setItem(request.item().trim());
		}
		if (request.quantity() != null && request.quantity() > 0) {
			sale.setQuantity(request.quantity());
		}
		if (request.unit_price() != null && request.unit_price().compareTo(BigDecimal.ZERO) > 0) {
			sale.setUnit_price(request.unit_price());
		}

		BigDecimal qty = BigDecimal.valueOf(sale.getQuantity());
		sale.setTotal_amount(sale.getUnit_price().multiply(qty));

		Sale saved = salesRepo.save(sale);
		return ResponseEntity.ok(new ApiResponse<>(saved, "Sale updated successfully ✅", true, Instant.now()));
	}

	public ResponseEntity<ApiResponse<Sale>> updateSale(String id, UpdateSaleRequest request) {
		return updateSale(id, request, null, null);
	}
}
