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
		if (paymentAmount == null || paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
			return ResponseEntity.status(400).body(new ApiResponse<>(null, "Payment amount must be greater than zero!", false, Instant.now()));
		}

		// Update cumulative customer accounts receivable totals
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
		customerRepo.save(customer);

		Sale targetSale = null;
		if (request.saleId() != null && !request.saleId().trim().isEmpty()) {
			targetSale = salesRepo.findById(request.saleId().trim()).orElse(null);
			if (targetSale != null) {
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
			}
		} else {
			// FIFO allocation across customer's unpaid sales
			List<Sale> customerSales = salesRepo.findByCustomerIdOrderByAddedOnAsc(customerId);
			BigDecimal remainingToAllocate = paymentAmount;
			for (Sale s : customerSales) {
				BigDecimal due = s.getBalanceDue() != null ? s.getBalanceDue() : BigDecimal.ZERO;
				if (due.compareTo(BigDecimal.ZERO) > 0) {
					BigDecimal alloc = remainingToAllocate.min(due);
					BigDecimal currentPaid = s.getAmountPaid() != null ? s.getAmountPaid() : BigDecimal.ZERO;
					s.setAmountPaid(currentPaid.add(alloc));
					BigDecimal newDue = due.subtract(alloc);
					s.setBalanceDue(newDue);
					s.setPaymentStatus(newDue.compareTo(BigDecimal.ZERO) == 0 ? "PAID_IN_FULL" : "PARTIAL_PAYMENT");
					salesRepo.save(s);

					remainingToAllocate = remainingToAllocate.subtract(alloc);
					if (remainingToAllocate.compareTo(BigDecimal.ZERO) <= 0) break;
				}
			}
		}

		long payCount = paymentRepo.count();
		String payId = "PAY-CUST-" + String.format("%04d", payCount + 1);
		while (paymentRepo.existsById(payId)) {
			payCount++;
			payId = "PAY-CUST-" + String.format("%04d", payCount + 1);
		}

		String paymentMode = request.paymentMode() != null && !request.paymentMode().trim().isEmpty() 
				? request.paymentMode().trim().toUpperCase() 
				: "CASH";
		LocalDate pDate = request.paymentDate() != null ? request.paymentDate() : LocalDate.now();

		CustomerPayment paymentRecord = new CustomerPayment(
			payId,
			customer,
			targetSale,
			paymentAmount,
			paymentMode,
			request.referenceNumber(),
			pDate,
			request.notes(),
			remainingDebt
		);
		paymentRepo.save(paymentRecord);

		return ResponseEntity.ok(new ApiResponse<>(customer, "Payment of KES " + paymentAmount + " recorded successfully. Remaining debt: KES " + remainingDebt + " ✅", true, Instant.now()));
	}
}
