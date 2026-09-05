package com.smartfarm.suppliers;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "suppliers")
public class Supplier {

	@Id
	private String id; // SUP-001, SUP-002

	@Column(unique = true, nullable = false)
	private String name;

	private String contactPerson;

	private String phoneNumber;

	private String email;

	private String idOrTaxNumber; // KRA PIN or National ID

	private String category; // "Fertilizer", "Feeds", "Seeds", "Tools", "Veterinary", "General"

	private String address;

	@Column(nullable = false)
	private BigDecimal totalBilled = BigDecimal.ZERO;

	@Column(nullable = false)
	private BigDecimal totalPaid = BigDecimal.ZERO;

	@Column(nullable = false)
	private BigDecimal balanceOwed = BigDecimal.ZERO; // Accounts Payable (totalBilled - totalPaid)

	private boolean isActive = true;

	@CreationTimestamp
	private LocalDateTime createdAt;

	public Supplier() {}

	public Supplier(String id, String name, String contactPerson, String phoneNumber, String email,
			String idOrTaxNumber, String category, String address) {
		this.id = id;
		this.name = name;
		this.contactPerson = contactPerson;
		this.phoneNumber = phoneNumber;
		this.email = email;
		this.idOrTaxNumber = idOrTaxNumber;
		this.category = category != null ? category : "General";
		this.address = address;
		this.totalBilled = BigDecimal.ZERO;
		this.totalPaid = BigDecimal.ZERO;
		this.balanceOwed = BigDecimal.ZERO;
		this.isActive = true;
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

	public String getContactPerson() {
		return contactPerson;
	}

	public void setContactPerson(String contactPerson) {
		this.contactPerson = contactPerson;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getIdOrTaxNumber() {
		return idOrTaxNumber;
	}

	public void setIdOrTaxNumber(String idOrTaxNumber) {
		this.idOrTaxNumber = idOrTaxNumber;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public BigDecimal getTotalBilled() {
		return totalBilled != null ? totalBilled : BigDecimal.ZERO;
	}

	public void setTotalBilled(BigDecimal totalBilled) {
		this.totalBilled = totalBilled != null ? totalBilled : BigDecimal.ZERO;
	}

	public BigDecimal getTotalPaid() {
		return totalPaid != null ? totalPaid : BigDecimal.ZERO;
	}

	public void setTotalPaid(BigDecimal totalPaid) {
		this.totalPaid = totalPaid != null ? totalPaid : BigDecimal.ZERO;
	}

	public BigDecimal getBalanceOwed() {
		return balanceOwed != null ? balanceOwed : BigDecimal.ZERO;
	}

	public void setBalanceOwed(BigDecimal balanceOwed) {
		this.balanceOwed = balanceOwed != null ? balanceOwed : BigDecimal.ZERO;
	}

	public boolean isActive() {
		return isActive;
	}

	public void setActive(boolean isActive) {
		this.isActive = isActive;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}
}
