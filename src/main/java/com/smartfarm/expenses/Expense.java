package com.smartfarm.expenses;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.hibernate.annotations.CreationTimestamp;

import com.smartfarm.projects.Project;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "expenses")
public class Expense {

	@Id
	private String id;
	@Column(nullable = false)
	private String title;
	@Column(nullable = false)
	private BigDecimal amount;
	private BigDecimal unitPrice;
	private BigDecimal quantity;
	@CreationTimestamp
	private LocalDate added_on;
	private String notes;
	@ManyToOne
	private Project project;
	
	public Expense() {}
	public Expense(String id, String title, BigDecimal amount, BigDecimal unitPrice, BigDecimal quantity, LocalDate added_on, String notes, Project project) {
		super();
		this.id = id;
		this.title = title;
		this.amount = amount;
		this.unitPrice = unitPrice;
		this.quantity = quantity;
		this.added_on = added_on;
		this.notes = notes;
		this.project = project;
	}
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public BigDecimal getAmount() {
		return amount;
	}
	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}
	public BigDecimal getUnitPrice() {
		return unitPrice;
	}
	public void setUnitPrice(BigDecimal unitPrice) {
		this.unitPrice = unitPrice;
	}
	public BigDecimal getQuantity() {
		return quantity;
	}
	public void setQuantity(BigDecimal quantity) {
		this.quantity = quantity;
	}
	public LocalDate getAdded_on() {
		return added_on;
	}
	public void setAdded_on(LocalDate added_on) {
		this.added_on = added_on;
	}
	public String getNotes() {
		return notes;
	}
	public void setNotes(String notes) {
		this.notes = notes;
	}
}
