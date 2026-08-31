package com.smartfarm.projects;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;

import com.smartfarm.category.Category;
import com.smartfarm.expenses.Expense;
import com.smartfarm.sales.Sale;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "projects")
public class Project {

	@Id
	private String id;
	@Column(unique = true, nullable = false)
	private String name;
	private String season; 
	private String status;
	@CreationTimestamp
	private LocalDate startDate; 
	private LocalDate endDate;
	private BigDecimal budget;
	private String description;
	@ManyToOne
	@JoinColumn(name = "category_id")
	private Category category;

	@ManyToOne
	@JoinColumn(name = "manager_id")
	private com.smartfarm.user.User manager;

	@ManyToOne
	@JoinColumn(name = "supervisor_id")
	private com.smartfarm.user.User supervisor;
	
	@OneToMany(mappedBy = "project")
	private List<Expense> expenses;
	
	@OneToMany(mappedBy = "project")
	private List<Sale> sales;
	
	public Project() {}
	public Project(String id, String name, String season, String status, LocalDate startDate, LocalDate endDate,
			BigDecimal budget, String description, Category category) {
		super();
		this.id = id;
		this.name = name;
		this.season = season;
		this.status = status;
		this.startDate = startDate;
		this.endDate = endDate;
		this.budget = budget;
		this.description = description;
		this.category = category;
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
	public String getSeason() {
		return season;
	}
	public void setSeason(String season) {
		this.season = season;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public LocalDate getStartDate() {
		return startDate;
	}
	public void setStartDate(LocalDate startDate) {
		this.startDate = startDate;
	}
	public LocalDate getEndDate() {
		return endDate;
	}
	public void setEndDate(LocalDate endDate) {
		this.endDate = endDate;
	}
	public BigDecimal getBudget() {
		return budget;
	}
	public void setBudget(BigDecimal budget) {
		this.budget = budget;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public Category getCategory() {
		return category;
	}
	public void setCategory(Category category) {
		this.category = category;
	}
	public com.smartfarm.user.User getManager() {
		return manager;
	}
	public void setManager(com.smartfarm.user.User manager) {
		this.manager = manager;
	}
	public com.smartfarm.user.User getSupervisor() {
		return supervisor;
	}
	public void setSupervisor(com.smartfarm.user.User supervisor) {
		this.supervisor = supervisor;
	}
	public List<Expense> getExpenses() {
		return expenses;
	}
	public void setExpenses(List<Expense> expenses) {
		this.expenses = expenses;
	}
}
