package com.smartfarm.employees;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "employees")
public class Employee {

	@Id
	private String id; // e.g. EMP-001, EMP-002

	@Column(nullable = false)
	private String fullName;

	@Column(unique = true, nullable = false)
	private String idNumber; // Kenyan National ID (7-8 digits) for adult verification

	private String phoneNumber;

	@Column(nullable = false)
	private String employmentType; // "CASUAL", "PERMANENT"

	@Column(nullable = false)
	private BigDecimal dailyRate; // Base daily wage

	@Column(nullable = false)
	private String status; // "ACTIVE", "INACTIVE"

	private String registeredById; // User ID of admin/manager who registered them

	@CreationTimestamp
	private LocalDateTime createdAt;

	public Employee() {}

	public Employee(String id, String fullName, String idNumber, String phoneNumber, String employmentType,
			BigDecimal dailyRate, String status, String registeredById) {
		this.id = id;
		this.fullName = fullName;
		this.idNumber = idNumber;
		this.phoneNumber = phoneNumber;
		this.employmentType = employmentType != null ? employmentType.toUpperCase() : "CASUAL";
		this.dailyRate = dailyRate != null ? dailyRate : BigDecimal.ZERO;
		this.status = status != null ? status.toUpperCase() : "ACTIVE";
		this.registeredById = registeredById;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getFullName() {
		return fullName;
	}

	public void setFullName(String fullName) {
		this.fullName = fullName;
	}

	public String getIdNumber() {
		return idNumber;
	}

	public void setIdNumber(String idNumber) {
		this.idNumber = idNumber;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	public String getEmploymentType() {
		return employmentType;
	}

	public void setEmploymentType(String employmentType) {
		this.employmentType = employmentType != null ? employmentType.toUpperCase() : "CASUAL";
	}

	public BigDecimal getDailyRate() {
		return dailyRate;
	}

	public void setDailyRate(BigDecimal dailyRate) {
		this.dailyRate = dailyRate;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status != null ? status.toUpperCase() : "ACTIVE";
	}

	public String getRegisteredById() {
		return registeredById;
	}

	public void setRegisteredById(String registeredById) {
		this.registeredById = registeredById;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}
}
