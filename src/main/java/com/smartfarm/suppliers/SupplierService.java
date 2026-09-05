package com.smartfarm.suppliers;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartfarm.ApiResponse;
import com.smartfarm.inventory.InventoryItem;
import com.smartfarm.inventory.InventoryItemRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class SupplierService {

	private final SupplierRepository supplierRepo;
	private final SupplierPurchaseRepository purchaseRepo;
	private final InventoryItemRepository inventoryRepo;

	public SupplierService(SupplierRepository supplierRepo, SupplierPurchaseRepository purchaseRepo,
			InventoryItemRepository inventoryRepo) {
		this.supplierRepo = supplierRepo;
		this.purchaseRepo = purchaseRepo;
		this.inventoryRepo = inventoryRepo;
	}

	public ResponseEntity<ApiResponse<List<Supplier>>> getAllSuppliers() {
		List<Supplier> list = supplierRepo.findAll();
		return ResponseEntity.ok(new ApiResponse<>(list, "Suppliers retrieved successfully ✅", true, Instant.now()));
	}

	public ResponseEntity<ApiResponse<Supplier>> getSupplierById(String id) {
		Supplier supplier = supplierRepo.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Supplier not found with ID: " + id));
		return ResponseEntity.ok(new ApiResponse<>(supplier, "Supplier retrieved successfully ✅", true, Instant.now()));
	}

	public ResponseEntity<ApiResponse<List<SupplierPurchase>>> getSupplierPurchases(String supplierId) {
		List<SupplierPurchase> list = purchaseRepo.findBySupplierIdOrderByPurchaseDateDesc(supplierId);
		return ResponseEntity.ok(new ApiResponse<>(list, "Supplier purchases retrieved ✅", true, Instant.now()));
	}

	@Transactional
	public ResponseEntity<ApiResponse<?>> createSupplier(CreateSupplierRequest req) {
		if (req.name() == null || req.name().trim().isEmpty()) {
			return ResponseEntity.status(400).body(new ApiResponse<>(null, "Supplier company or trader name is required!", false, Instant.now()));
		}
		if (supplierRepo.existsByNameIgnoreCase(req.name().trim())) {
			return ResponseEntity.status(400).body(new ApiResponse<>(null, "A supplier with this name already exists!", false, Instant.now()));
		}

		long count = supplierRepo.count();
		String supId = "SUP-" + String.format("%03d", count + 1);

		Supplier supplier = new Supplier(
			supId,
			req.name().trim(),
			req.contactPerson() != null ? req.contactPerson().trim() : "",
			req.phoneNumber() != null ? req.phoneNumber().trim() : "",
			req.email() != null ? req.email().trim() : "",
			req.idOrTaxNumber() != null ? req.idOrTaxNumber().trim() : "",
			req.category() != null ? req.category().trim() : "General",
			req.address() != null ? req.address().trim() : ""
		);

		Supplier saved = supplierRepo.save(supplier);
		return ResponseEntity.status(201).body(new ApiResponse<>(saved, "Supplier registered successfully ✅", true, Instant.now()));
	}

	@Transactional
	public ResponseEntity<ApiResponse<?>> recordPurchase(SupplierPurchaseRequest req) {
		Supplier supplier = supplierRepo.findById(req.supplierId())
				.orElseThrow(() -> new EntityNotFoundException("Supplier not found with ID: " + req.supplierId()));

		BigDecimal invoiceAmount = req.invoiceAmount() != null ? req.invoiceAmount() : BigDecimal.ZERO;
		BigDecimal amountPaid = req.amountPaid() != null ? req.amountPaid() : BigDecimal.ZERO;

		if (invoiceAmount.compareTo(BigDecimal.ZERO) <= 0) {
			return ResponseEntity.status(400).body(new ApiResponse<>(null, "Invoice amount must be greater than zero!", false, Instant.now()));
		}

		BigDecimal balanceDue = invoiceAmount.subtract(amountPaid);
		if (balanceDue.compareTo(BigDecimal.ZERO) < 0) {
			balanceDue = BigDecimal.ZERO;
		}

		String status = "UNPAID";
		if (amountPaid.compareTo(invoiceAmount) >= 0) {
			status = "PAID";
		} else if (amountPaid.compareTo(BigDecimal.ZERO) > 0) {
			status = "PARTIAL";
		}

		InventoryItem item = null;
		if (req.inventoryItemId() != null && !req.inventoryItemId().trim().isEmpty()) {
			item = inventoryRepo.findById(req.inventoryItemId().trim()).orElse(null);
			if (item != null && req.restockQuantity() != null && req.restockQuantity().compareTo(BigDecimal.ZERO) > 0) {
				BigDecimal currentStock = item.getQuantityInStock() != null ? item.getQuantityInStock() : BigDecimal.ZERO;
				item.setQuantityInStock(currentStock.add(req.restockQuantity()));
				inventoryRepo.save(item);
			}
		}

		long count = purchaseRepo.count();
		String purId = "PUR-" + String.format("%03d", count + 1);

		SupplierPurchase purchase = new SupplierPurchase(
			purId,
			supplier,
			item,
			req.invoiceNumber() != null ? req.invoiceNumber().trim() : ("INV-" + purId),
			invoiceAmount,
			amountPaid,
			balanceDue,
			status,
			req.purchaseDate() != null ? req.purchaseDate() : LocalDate.now(),
			req.dueDate(),
			req.notes()
		);

		SupplierPurchase saved = purchaseRepo.save(purchase);

		// Update supplier cumulative Accounts Payable
		supplier.setTotalBilled(supplier.getTotalBilled().add(invoiceAmount));
		supplier.setTotalPaid(supplier.getTotalPaid().add(amountPaid));
		supplier.setBalanceOwed(supplier.getTotalBilled().subtract(supplier.getTotalPaid()));
		supplierRepo.save(supplier);

		return ResponseEntity.status(201).body(new ApiResponse<>(saved, "Supplier purchase recorded & stock updated ✅", true, Instant.now()));
	}

	@Transactional
	public ResponseEntity<ApiResponse<?>> recordPayment(String supplierId, SupplierPaymentRequest req) {
		Supplier supplier = supplierRepo.findById(supplierId)
				.orElseThrow(() -> new EntityNotFoundException("Supplier not found with ID: " + supplierId));

		BigDecimal payment = req.amount() != null ? req.amount() : BigDecimal.ZERO;
		if (payment.compareTo(BigDecimal.ZERO) <= 0) {
			return ResponseEntity.status(400).body(new ApiResponse<>(null, "Payment amount must be greater than zero!", false, Instant.now()));
		}

		supplier.setTotalPaid(supplier.getTotalPaid().add(payment));
		BigDecimal newBalance = supplier.getTotalBilled().subtract(supplier.getTotalPaid());
		if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
			newBalance = BigDecimal.ZERO;
		}
		supplier.setBalanceOwed(newBalance);
		Supplier updated = supplierRepo.save(supplier);

		return ResponseEntity.ok(new ApiResponse<>(updated, "Supplier payment recorded. Remaining debt balance: KES " + newBalance + " ✅", true, Instant.now()));
	}
}
