package com.smartfarm.user;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.smartfarm.category.Category;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {

	@Id
	private String id;
	@Column(unique = true, nullable = false)
	private String username;
	@Column(unique = true)
	private String email;
	@JsonIgnore
	private String password;
	@Column(nullable = false)
	private String role; // "ADMIN", "MANAGER", "SUPERVISOR"
	@Column(nullable = false)
	private String status; // "ACTIVE", "PENDING_APPROVAL", "DISABLED"
	private String createdById; // Tracks which Admin/Manager created this user

	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(
		name = "user_assigned_categories",
		joinColumns = @JoinColumn(name = "user_id"), 
		inverseJoinColumns = @JoinColumn(name = "category_id")
	)
	private Set<Category> assignedCategories = new HashSet<>();
	
	public User() {}

	public User(String id, String username, String email, String password, String role, String status, String createdById) {
		this.id = id;
		this.username = username;
		this.email = email;
		this.password = password;
		this.role = role != null ? role.toUpperCase() : "MANAGER";
		this.status = status != null ? status.toUpperCase() : "ACTIVE";
		this.createdById = createdById;
	}

	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public String getRole() {
		return role;
	}
	public void setRole(String role) {
		this.role = role != null ? role.toUpperCase() : "MANAGER";
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status != null ? status.toUpperCase() : "ACTIVE";
	}
	public String getCreatedById() {
		return createdById;
	}
	public void setCreatedById(String createdById) {
		this.createdById = createdById;
	}
	public Set<Category> getAssignedCategories() {
		return assignedCategories;
	}
	public void setAssignedCategories(Set<Category> assignedCategories) {
		this.assignedCategories = assignedCategories;
	}
}
