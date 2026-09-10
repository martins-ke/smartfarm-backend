package com.smartfarm.customers;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.hibernate.annotations.CreationTimestamp;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.smartfarm.sales.Sale;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "customer_payments")
public class CustomerPayment {

	@Id
	private String id; // e.g. PAY-CUST-001

	@ManyToOne
	@JoinColumn(name = "customer_id", nullable = false)
	private Customer customer;

	@ManyToOne
	@JoinColumn(name = "sale_id")
	private Sale sale; // Optional: linked specific sale invoice

	@Column(nullable = false)
	private BigDecimal amount = BigDecimal.ZERO;

	@Column(nullable = false)
	private String paymentMode = "CASH"; // "CASH", "MPESA", "BANK_TRANSFER"

	private String referenceNumber; // Transaction reference code / receipt number

	@Column(nullable = false)
	private LocalDate paymentDate = LocalDate.now();

	private String notes;

	@Column(nullable = false)
	private BigDecimal balanceAfter = BigDecimal.ZERO; // Customer or sale balance after this payment

	@CreationTimestamp
	@JsonProperty("created_at")
	private Instant createdAt;

	public CustomerPayment() {}

	public CustomerPayment(String id, Customer customer, Sale sale, BigDecimal amount, String paymentMode,
			String referenceNumber, LocalDate paymentDate, String notes, BigDecimal balanceAfter) {
		this.id = id;
		this.customer = customer;
		this.sale = sale;
		this.amount = amount != null ? amount : BigDecimal.ZERO;
		this.paymentMode = paymentMode != null ? paymentMode.toUpperCase() : "CASH";
		this.referenceNumber = referenceNumber;
		this.paymentDate = paymentDate != null ? paymentDate : LocalDate.now();
		this.notes = notes;
		this.balanceAfter = balanceAfter != null ? balanceAfter : BigDecimal.ZERO;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Customer getCustomer() {
		return customer;
	}

	public void setCustomer(Customer customer) {
		this.customer = customer;
	}

	public Sale getSale() {
		return sale;
	}

	public void setSale(Sale sale) {
		this.sale = sale;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	public String getPaymentMode() {
		return paymentMode;
	}

	public void setPaymentMode(String paymentMode) {
		this.paymentMode = paymentMode;
	}

	public String getReferenceNumber() {
		return referenceNumber;
	}

	public void setReferenceNumber(String referenceNumber) {
		this.referenceNumber = referenceNumber;
	}

	public LocalDate getPaymentDate() {
		return paymentDate;
	}

	public void setPaymentDate(LocalDate paymentDate) {
		this.paymentDate = paymentDate;
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}

	public BigDecimal getBalanceAfter() {
		return balanceAfter;
	}

	public void setBalanceAfter(BigDecimal balanceAfter) {
		this.balanceAfter = balanceAfter;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}
}
