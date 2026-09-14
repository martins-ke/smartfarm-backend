package com.smartfarm.customers;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartfarm.ApiResponse;
import com.smartfarm.sales.Sale;
import com.smartfarm.sales.SalesRepository;
import com.smartfarm.util.IdGenarator;

import jakarta.persistence.EntityNotFoundException;

@Service
public class CustomerService {

	private final CustomerRepository customerRepo;
	private final CustomerPaymentRepository paymentRepo;
	private final SalesRepository salesRepo;
	private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;
	
	public CustomerService(CustomerRepository customerRepo, CustomerPaymentRepository paymentRepo,
			SalesRepository salesRepo, org.springframework.jdbc.core.JdbcTemplate jdbcTemplate) {
		this.customerRepo = customerRepo;
		this.paymentRepo = paymentRepo;
		this.salesRepo = salesRepo;
		this.jdbcTemplate = jdbcTemplate;
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

		String idNumber = (request.id_number() != null && !request.id_number().trim().isEmpty())
				? request.id_number().trim()
				: null;

		String contact = (request.contact() != null && !request.contact().trim().isEmpty())
				? request.contact().trim()
				: null;

		BigDecimal creditLimit = request.credit_limit() != null ? request.credit_limit() : BigDecimal.ZERO;
		
		Customer customer = new Customer(id, request.name().trim(), contact, idNumber, address, true, creditLimit);
		if (request.category() != null && !request.category().trim().isEmpty()) {
			customer.setCategory(request.category().trim());
		}
		Customer saved = customerRepo.save(customer);

		// Mirror to legacy singular customer table to ensure foreign keys on older MySQL schemas resolve without error
		try {
			jdbcTemplate.update(
				"INSERT INTO customer (id, name, contact, id_number, address, is_active) VALUES (?, ?, ?, ?, ?, ?) "
				+ "ON DUPLICATE KEY UPDATE name = VALUES(name), contact = VALUES(contact), id_number = VALUES(id_number), address = VALUES(address)",
				saved.getId(), saved.getName(), saved.getContact(), saved.getIdNumber(), saved.getAddress(), saved.isActive()
			);
		} catch (Exception e) {
			// If legacy table doesn't exist or isn't needed, safe to ignore
		}

		return saved;
	}
	
	public ResponseEntity<ApiResponse<List<Customer>>> getAllCustomers() {
		return ResponseEntity.status(200).body(new ApiResponse<>(customerRepo.findAll(), "Customer list fetched successfully ✅", true, Instant.now()));
	}

	public ResponseEntity<ApiResponse<Customer>> getCustomerById(String id) {
		Customer customer = customerRepo.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Customer not found with ID: " + id));
		return ResponseEntity.ok(new ApiResponse<>(customer, "Customer retrieved successfully ✅", true, Instant.now()));
	}

	public ResponseEntity<ApiResponse<List<CustomerPayment>>> getCustomerPayments(String customerId) {
		List<CustomerPayment> payments = paymentRepo.findByCustomerIdOrderByPaymentDateDescCreatedAtDesc(customerId);
		return ResponseEntity.ok(new ApiResponse<>(payments, "Customer payments retrieved ✅", true, Instant.now()));
	}

	public ResponseEntity<ApiResponse<List<CustomerPayment>>> getSalePayments(String saleId) {
		List<CustomerPayment> payments = paymentRepo.findBySaleIdOrderByPaymentDateDescCreatedAtDesc(saleId);
		return ResponseEntity.ok(new ApiResponse<>(payments, "Sale payment history retrieved ✅", true, Instant.now()));
	}

	@Transactional
	public ResponseEntity<ApiResponse<?>> settleCustomerDebt(String customerId, CustomerPaymentRequest request) {
		Customer customer = customerRepo.findById(customerId)
				.orElseThrow(() -> new EntityNotFoundException("Customer not found with ID: " + customerId));

		BigDecimal paymentAmount = request.amount();
		Sale targetSale = salesRepo.findById(request.saleId().trim())
				.orElseThrow(() -> new EntityNotFoundException("Sale invoice not found with ID: " + request.saleId()));

		if (targetSale.getCustomer() == null || !customerId.equals(targetSale.getCustomer().getId())) {
			return ResponseEntity.status(400).body(new ApiResponse<>(null, "Sale invoice " + request.saleId() + " does not belong to customer " + customer.getName(), false, Instant.now()));
		}

		// 1. Update target Sale in sales table
		BigDecimal currentPaid = targetSale.getAmountPaid() != null ? targetSale.getAmountPaid() : BigDecimal.ZERO;
		BigDecimal totalAmount = targetSale.getTotal_amount() != null ? targetSale.getTotal_amount() : BigDecimal.ZERO;
		BigDecimal newSalePaid = currentPaid.add(paymentAmount);
		if (newSalePaid.compareTo(totalAmount) > 0) {
			newSalePaid = totalAmount;
		}
		targetSale.setAmountPaid(newSalePaid);
		BigDecimal newDue = totalAmount.subtract(newSalePaid);
		if (newDue.compareTo(BigDecimal.ZERO) < 0) newDue = BigDecimal.ZERO;
		targetSale.setBalanceDue(newDue);
		targetSale.setPaymentStatus(newDue.compareTo(BigDecimal.ZERO) == 0 ? "PAID_IN_FULL" : "PARTIAL_PAYMENT");
		salesRepo.save(targetSale);

		// 2. Update Customer cumulative accounts receivable totals
		customer.setTotalPaid(customer.getTotalPaid().add(paymentAmount));
		BigDecimal remainingDebt = customer.getTotalPurchases().subtract(customer.getTotalPaid());
		if (remainingDebt.compareTo(BigDecimal.ZERO) < 0) {
			remainingDebt = BigDecimal.ZERO;
		}
		customer.setOutstandingDebt(remainingDebt);
		customer.setCreditStatus(remainingDebt.compareTo(BigDecimal.ZERO) == 0 ? "CLEAR"
				: (customer.getCreditLimit().compareTo(BigDecimal.ZERO) > 0 && remainingDebt.compareTo(customer.getCreditLimit()) > 0 ? "BLOCKED" : "HAS_DEBT"));
		customerRepo.save(customer);

		// 3. Record transaction in customer_payments audit table
		long payCount = paymentRepo.count();
		String payId = "PAY-CUST-" + String.format("%04d", payCount + 1);
		while (paymentRepo.existsById(payId)) {
			payCount++;
			payId = "PAY-CUST-" + String.format("%04d", payCount + 1);
		}

		String paymentMode = request.paymentMode() != null && !request.paymentMode().trim().isEmpty() 
				? request.paymentMode().trim().toUpperCase() 
				: "CASH";

		CustomerPayment paymentRecord = new CustomerPayment(
			payId,
			customer,
			targetSale,
			paymentAmount,
			paymentMode,
			request.referenceNumber(),
			LocalDate.now(),
			request.notes(),
			remainingDebt
		);
		paymentRepo.save(paymentRecord);

		return ResponseEntity.ok(new ApiResponse<>(customer, "Payment of KES " + paymentAmount + " recorded for invoice " + targetSale.getId() + ". Remaining debt: KES " + remainingDebt + " ✅", true, Instant.now()));
	}
}
