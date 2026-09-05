package com.smartfarm.customers;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.smartfarm.sales.Sale;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "customers")
public class Customer {

	@Id
	private String id;
	private String name;
	@Column(unique = true)
	private String contact;
	@Column(unique = true) 
	@JsonProperty("id_number")
	private String idNumber;
	private String address;
	private boolean isActive = true;

	@Column(nullable = false)
	private BigDecimal creditLimit = BigDecimal.ZERO;

	@Column(nullable = false)
	private BigDecimal totalPurchases = BigDecimal.ZERO;

	@Column(nullable = false)
	private BigDecimal totalPaid = BigDecimal.ZERO;

	@Column(nullable = false)
	private BigDecimal outstandingDebt = BigDecimal.ZERO; // Accounts Receivable (totalPurchases - totalPaid)

	@Column(nullable = false)
	private String creditStatus = "CLEAR"; // "CLEAR", "HAS_DEBT", "BLOCKED"

	private String category = "General Buyer";
	
	@OneToMany(mappedBy = "customer")
	@JsonIgnore
	private List<Sale> sales;
	
	public Customer() {}

	public Customer(String id, String name, String contact, String idNumber, String address, boolean isActive) {
		this.id = id;
		this.name = name;
		this.contact = contact;
		this.idNumber = idNumber;
		this.address = address;
		this.isActive = isActive;
		this.creditLimit = BigDecimal.ZERO;
		this.totalPurchases = BigDecimal.ZERO;
		this.totalPaid = BigDecimal.ZERO;
		this.outstandingDebt = BigDecimal.ZERO;
		this.creditStatus = "CLEAR";
	}

	public Customer(String id, String name, String contact, String idNumber, String address, boolean isActive,
			BigDecimal creditLimit) {
		this.id = id;
		this.name = name;
		this.contact = contact;
		this.idNumber = idNumber;
		this.address = address;
		this.isActive = isActive;
		this.creditLimit = creditLimit != null ? creditLimit : BigDecimal.ZERO;
		this.totalPurchases = BigDecimal.ZERO;
		this.totalPaid = BigDecimal.ZERO;
		this.outstandingDebt = BigDecimal.ZERO;
		this.creditStatus = "CLEAR";
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getContact() {
		return contact;
	}

	public void setContact(String contact) {
		this.contact = contact;
	}

	public String getIdNumber() {
		return idNumber;
	}

	public void setIdNumber(String idNumber) {
		this.idNumber = idNumber;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public boolean isActive() {
		return isActive;
	}

	public void setActive(boolean isActive) {
		this.isActive = isActive;
	}

	public BigDecimal getCreditLimit() {
		return creditLimit != null ? creditLimit : BigDecimal.ZERO;
	}

	public void setCreditLimit(BigDecimal creditLimit) {
		this.creditLimit = creditLimit != null ? creditLimit : BigDecimal.ZERO;
	}

	public BigDecimal getTotalPurchases() {
		return totalPurchases != null ? totalPurchases : BigDecimal.ZERO;
	}

	public void setTotalPurchases(BigDecimal totalPurchases) {
		this.totalPurchases = totalPurchases != null ? totalPurchases : BigDecimal.ZERO;
	}

	public BigDecimal getTotalPaid() {
		return totalPaid != null ? totalPaid : BigDecimal.ZERO;
	}

	public void setTotalPaid(BigDecimal totalPaid) {
		this.totalPaid = totalPaid != null ? totalPaid : BigDecimal.ZERO;
	}

	public BigDecimal getOutstandingDebt() {
		return outstandingDebt != null ? outstandingDebt : BigDecimal.ZERO;
	}

	public void setOutstandingDebt(BigDecimal outstandingDebt) {
		this.outstandingDebt = outstandingDebt != null ? outstandingDebt : BigDecimal.ZERO;
	}

	public String getCreditStatus() {
		return creditStatus != null ? creditStatus : "CLEAR";
	}

	public void setCreditStatus(String creditStatus) {
		this.creditStatus = creditStatus != null ? creditStatus : "CLEAR";
	}

	public List<Sale> getSales() {
		return sales;
	}

	public void setSales(List<Sale> sales) {
		this.sales = sales;
	}

	public String getCategory() {
		return category != null ? category : "General Buyer";
	}

	public void setCategory(String category) {
		this.category = category;
	}
}
