package com.smartfarm.sales;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.hibernate.annotations.CreationTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.smartfarm.customers.Customer;
import com.smartfarm.projects.Project;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "sales")
public class Sale {

	@Id
	private String id;
	@Column(nullable = false)
	private String item;
	@Column(nullable = false) 
	private float quantity;
	@Column(nullable = false)
	private BigDecimal unit_price;
	@CreationTimestamp
	@Column(name = "added_on")
	@JsonProperty("added_on")
	private LocalDate addedOn;
	private BigDecimal total_amount;

	private BigDecimal amountPaid;
	private BigDecimal balanceDue;
	private String paymentMode; // "CASH", "MPESA", "BANK_TRANSFER", "CREDIT_LEDGER"
	private String paymentStatus; // "PAID_IN_FULL", "PARTIAL_PAYMENT", "CREDIT_UNPAID"

	@ManyToOne
	@JoinColumn(name = "project_id")
	@JsonIgnore
	private Project project;

	@ManyToOne
	@JoinColumn(name = "customer_id")
	private Customer customer;
	
	public Sale() {}

	public Sale(String id, String item, float quantity, BigDecimal unit_price, LocalDate added_on,
			BigDecimal total_amount, Project project, Customer customer) {
		this.id = id;
		this.item = item;
		this.quantity = quantity;
		this.unit_price = unit_price;
		this.customer = customer;
		this.addedOn = added_on;
		this.total_amount = total_amount;
		this.amountPaid = total_amount;
		this.balanceDue = BigDecimal.ZERO;
		this.paymentMode = "CASH";
		this.paymentStatus = "PAID_IN_FULL";
		this.project = project;
	}

	public Sale(String id, String item, float quantity, BigDecimal unit_price, LocalDate added_on,
			BigDecimal total_amount, BigDecimal amountPaid, BigDecimal balanceDue, String paymentMode,
			String paymentStatus, Project project, Customer customer) {
		this.id = id;
		this.item = item;
		this.quantity = quantity;
		this.unit_price = unit_price;
		this.addedOn = added_on;
		this.total_amount = total_amount;
		this.amountPaid = amountPaid != null ? amountPaid : total_amount;
		this.balanceDue = balanceDue != null ? balanceDue : BigDecimal.ZERO;
		this.paymentMode = paymentMode != null ? paymentMode : "CASH";
		this.paymentStatus = paymentStatus != null ? paymentStatus : "PAID_IN_FULL";
		this.project = project;
		this.customer = customer;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getItem() {
		return item;
	}

	public void setItem(String item) {
		this.item = item;
	}

	public float getQuantity() {
		return quantity;
	}

	public void setQuantity(float quantity) {
		this.quantity = quantity;
	}

	public BigDecimal getUnit_price() {
		return unit_price;
	}

	public void setUnit_price(BigDecimal unit_price) {
		this.unit_price = unit_price;
	}

	public Customer getCustomer() {
		return customer;
	}

	public void setCustomer(Customer customer) {
		this.customer = customer;
	}

	public LocalDate getAddedOn() {
		return addedOn;
	}

	public void setAddedOn(LocalDate addedOn) {
		this.addedOn = addedOn;
	}

	@JsonProperty("added_on")
	public LocalDate getAdded_on() {
		return addedOn;
	}

	public void setAdded_on(LocalDate added_on) {
		this.addedOn = added_on;
	}

	public BigDecimal getTotal_amount() {
		return total_amount;
	}

	public void setTotal_amount(BigDecimal total_amount) {
		this.total_amount = total_amount;
	}

	public BigDecimal getAmountPaid() {
		return amountPaid != null ? amountPaid : total_amount;
	}

	public void setAmountPaid(BigDecimal amountPaid) {
		this.amountPaid = amountPaid;
	}

	public BigDecimal getBalanceDue() {
		return balanceDue != null ? balanceDue : BigDecimal.ZERO;
	}

	public void setBalanceDue(BigDecimal balanceDue) {
		this.balanceDue = balanceDue;
	}

	public String getPaymentMode() {
		return paymentMode != null ? paymentMode : "CASH";
	}

	public void setPaymentMode(String paymentMode) {
		this.paymentMode = paymentMode;
	}

	public String getPaymentStatus() {
		return paymentStatus != null ? paymentStatus : "PAID_IN_FULL";
	}

	public void setPaymentStatus(String paymentStatus) {
		this.paymentStatus = paymentStatus;
	}

	public Project getProject() {
		return project;
	}

	public void setProject(Project project) {
		this.project = project;
	}

	@JsonProperty("project_id")
	public String getProjectId() {
		return project != null ? project.getId() : null;
	}

	@JsonProperty("projectName")
	public String getProjectName() {
		return project != null ? project.getName() : null;
	}

	@JsonProperty("categoryName")
	public String getCategoryName() {
		return project != null && project.getCategory() != null ? project.getCategory().getName() : null;
	}
}
