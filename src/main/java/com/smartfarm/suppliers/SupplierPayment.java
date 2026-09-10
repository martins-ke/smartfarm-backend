package com.smartfarm.suppliers;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.hibernate.annotations.CreationTimestamp;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "supplier_payments")
public class SupplierPayment {

	@Id
	private String id; // e.g. PAY-SUP-001

	@ManyToOne
	@JoinColumn(name = "supplier_id", nullable = false)
	private Supplier supplier;

	@ManyToOne
	@JoinColumn(name = "purchase_id")
	private SupplierPurchase purchase; // Optional: linked specific purchase invoice

	@Column(nullable = false)
	private BigDecimal amount = BigDecimal.ZERO;

	@Column(nullable = false)
	private String paymentMode = "MPESA"; // "MPESA", "BANK_TRANSFER", "CASH"

	private String referenceNumber; // Transaction reference / cheque / EFT code

	@Column(nullable = false)
	private LocalDate paymentDate = LocalDate.now();

	private String notes;

	@Column(nullable = false)
	private BigDecimal balanceAfter = BigDecimal.ZERO; // Supplier or purchase balance remaining after this payment

	@CreationTimestamp
	@JsonProperty("created_at")
	private Instant createdAt;

	public SupplierPayment() {}

	public SupplierPayment(String id, Supplier supplier, SupplierPurchase purchase, BigDecimal amount,
			String paymentMode, String referenceNumber, LocalDate paymentDate, String notes, BigDecimal balanceAfter) {
		this.id = id;
		this.supplier = supplier;
		this.purchase = purchase;
		this.amount = amount != null ? amount : BigDecimal.ZERO;
		this.paymentMode = paymentMode != null ? paymentMode.toUpperCase() : "MPESA";
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

	public Supplier getSupplier() {
		return supplier;
	}

	public void setSupplier(Supplier supplier) {
		this.supplier = supplier;
	}

	public SupplierPurchase getPurchase() {
		return purchase;
	}

	public void setPurchase(SupplierPurchase purchase) {
		this.purchase = purchase;
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
