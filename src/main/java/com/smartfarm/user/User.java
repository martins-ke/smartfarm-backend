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
	private String managerId; // Tracks parent Manager for dedicated 1:N supervisors
	private int maxProjectCapacity = 4; // Default max project capacity for supervisors

	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(
		name = "user_assigned_categories",
		joinColumns = @JoinColumn(name = "user_id"), 
		inverseJoinColumns = @JoinColumn(name = "category_id")
	)
	private Set<Category> assignedCategories = new HashSet<>();

	@Column(name = "privileges", length = 500)
	private String privilegesRaw;
	
	public User() {}

	public User(String id, String username, String email, String password, String role, String status, String createdById) {
		this.id = id;
		this.username = username;
		this.email = email;
		this.password = password;
		this.role = role != null ? role.toUpperCase() : "MANAGER";
		this.status = status != null ? status.toUpperCase() : "ACTIVE";
		this.createdById = createdById;
		initDefaultPrivileges();
	}

	public void initDefaultPrivileges() {
		Set<String> privs = new HashSet<>();
		if ("MANAGER".equalsIgnoreCase(this.role)) {
			privs.add("CAN_CREATE_SUPERVISORS");
			privs.add("CAN_VIEW_FINANCIALS");
		} else if ("SUPERVISOR".equalsIgnoreCase(this.role)) {
			privs.add("CAN_RECORD_HARVEST");
			privs.add("CAN_LOG_ACTIVITIES");
			privs.add("CAN_USE_INVENTORY");
		}
		setPrivileges(privs);
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
	public String getManagerId() {
		return managerId;
	}
	public void setManagerId(String managerId) {
		this.managerId = managerId;
	}
	public int getMaxProjectCapacity() {
		return maxProjectCapacity <= 0 ? 4 : maxProjectCapacity;
	}
	public void setMaxProjectCapacity(int maxProjectCapacity) {
		this.maxProjectCapacity = maxProjectCapacity <= 0 ? 4 : maxProjectCapacity;
	}
	public Set<String> getPrivileges() {
		Set<String> result = new HashSet<>();
		if (this.privilegesRaw != null) {
			if (!this.privilegesRaw.trim().isEmpty() && !"NONE".equalsIgnoreCase(this.privilegesRaw.trim())) {
				for (String p : this.privilegesRaw.split(",")) {
					String trimmed = p.trim();
					if (!trimmed.isEmpty() && !"NONE".equalsIgnoreCase(trimmed)) {
						result.add(trimmed);
					}
				}
			}
			return result;
		}

		// Only fall back to defaults if privilegesRaw has NEVER been set (is null)
		if ("MANAGER".equalsIgnoreCase(this.role)) {
			result.add("CAN_CREATE_SUPERVISORS");
			result.add("CAN_VIEW_FINANCIALS");
		} else if ("SUPERVISOR".equalsIgnoreCase(this.role)) {
			result.add("CAN_RECORD_HARVEST");
			result.add("CAN_LOG_ACTIVITIES");
			result.add("CAN_USE_INVENTORY");
		}
		return result;
	}
	public void setPrivileges(Set<String> privileges) {
		if (privileges == null || privileges.isEmpty()) {
			this.privilegesRaw = "NONE";
		} else {
			this.privilegesRaw = String.join(",", privileges);
		}
	}
}
