package com.smartfarm.suppliers;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.hibernate.annotations.CreationTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.smartfarm.inventory.InventoryItem;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "supplier_purchases")
public class SupplierPurchase {

	@Id
	private String id; // PUR-001, PUR-002

	@ManyToOne
	@JoinColumn(name = "supplier_id", nullable = false)
	private Supplier supplier;

	@ManyToOne
	@JoinColumn(name = "inventory_item_id")
	private InventoryItem inventoryItem;

	private String invoiceNumber;

	@Column(nullable = false)
	private BigDecimal invoiceAmount;

	@Column(nullable = false)
	private BigDecimal amountPaid;

	@Column(nullable = false)
	private BigDecimal balanceDue; // invoiceAmount - amountPaid

	@Column(nullable = false)
	private String paymentStatus; // "PAID", "PARTIAL", "UNPAID"

	@CreationTimestamp
	private LocalDate purchaseDate;

	private LocalDate dueDate;

	private String notes;

	public SupplierPurchase() {}

	public SupplierPurchase(String id, Supplier supplier, InventoryItem inventoryItem, String invoiceNumber,
			BigDecimal invoiceAmount, BigDecimal amountPaid, BigDecimal balanceDue, String paymentStatus,
			LocalDate purchaseDate, LocalDate dueDate, String notes) {
		this.id = id;
		this.supplier = supplier;
		this.inventoryItem = inventoryItem;
		this.invoiceNumber = invoiceNumber;
		this.invoiceAmount = invoiceAmount != null ? invoiceAmount : BigDecimal.ZERO;
		this.amountPaid = amountPaid != null ? amountPaid : BigDecimal.ZERO;
		this.balanceDue = balanceDue != null ? balanceDue : BigDecimal.ZERO;
		this.paymentStatus = paymentStatus != null ? paymentStatus.toUpperCase() : "UNPAID";
		this.purchaseDate = purchaseDate != null ? purchaseDate : LocalDate.now();
		this.dueDate = dueDate;
		this.notes = notes;
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

	public InventoryItem getInventoryItem() {
		return inventoryItem;
	}

	public void setInventoryItem(InventoryItem inventoryItem) {
		this.inventoryItem = inventoryItem;
	}

	public String getInvoiceNumber() {
		return invoiceNumber;
	}

	public void setInvoiceNumber(String invoiceNumber) {
		this.invoiceNumber = invoiceNumber;
	}

	public BigDecimal getInvoiceAmount() {
		return invoiceAmount;
	}

	public void setInvoiceAmount(BigDecimal invoiceAmount) {
		this.invoiceAmount = invoiceAmount;
	}

	public BigDecimal getAmountPaid() {
		return amountPaid;
	}

	public void setAmountPaid(BigDecimal amountPaid) {
		this.amountPaid = amountPaid;
	}

	public BigDecimal getBalanceDue() {
		return balanceDue;
	}

	public void setBalanceDue(BigDecimal balanceDue) {
		this.balanceDue = balanceDue;
	}

	public String getPaymentStatus() {
		return paymentStatus;
	}

	public void setPaymentStatus(String paymentStatus) {
		this.paymentStatus = paymentStatus != null ? paymentStatus.toUpperCase() : "UNPAID";
	}

	public LocalDate getPurchaseDate() {
		return purchaseDate;
	}

	public void setPurchaseDate(LocalDate purchaseDate) {
		this.purchaseDate = purchaseDate;
	}

	public LocalDate getDueDate() {
		return dueDate;
	}

	public void setDueDate(LocalDate dueDate) {
		this.dueDate = dueDate;
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}
}
