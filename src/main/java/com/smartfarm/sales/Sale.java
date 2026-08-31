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
	private LocalDate added_on;
	private BigDecimal total_amount;

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
		super();
		this.id = id;
		this.item = item;
		this.quantity = quantity;
		this.unit_price = unit_price;
		this.customer = customer;
		this.added_on = added_on;
		this.total_amount = total_amount;
		this.project = project;
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
	public LocalDate getAdded_on() {
		return added_on;
	}
	public void setAdded_on(LocalDate added_on) {
		this.added_on = added_on;
	}
	public BigDecimal getTotal_amount() {
		return total_amount;
	}
	public void setTotal_amount(BigDecimal total_amount) {
		this.total_amount = total_amount;
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
}
