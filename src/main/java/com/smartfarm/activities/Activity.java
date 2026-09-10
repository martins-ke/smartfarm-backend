package com.smartfarm.activities;

import java.time.LocalDate;

import org.hibernate.annotations.CreationTimestamp;

import com.smartfarm.projects.Project;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "activities")
public class Activity {

	@Id
	private String id;
	@Column(nullable = false)
	private String title;
	private String type;
	@CreationTimestamp
	private LocalDate added_on;
	private String notes;
	private LocalDate scheduledDate;
	private LocalDate dueDate;
	private String status = "SCHEDULED"; // "SCHEDULED", "IN_PROGRESS", "COMPLETED", "CANCELLED"
	private String priority = "MEDIUM"; // "LOW", "MEDIUM", "HIGH", "URGENT"
	private LocalDate completedOn;

	@ManyToOne
	@JoinColumn(name = "project_id")
	@com.fasterxml.jackson.annotation.JsonIgnore
	private Project project;

	@com.fasterxml.jackson.annotation.JsonProperty("project_id")
	public String getProjectId() {
		return project != null ? project.getId() : null;
	}
	
	public Activity() {}

	public Activity(String id, String title, String type, LocalDate added_on, String notes, Project project) {
		super();
		this.id = id;
		this.title = title;
		this.type = type;
		this.added_on = added_on;
		this.notes = notes;
		this.project = project;
		this.status = "COMPLETED";
		this.priority = "MEDIUM";
		this.scheduledDate = added_on;
	}

	public Activity(String id, String title, String type, LocalDate added_on, String notes,
			LocalDate scheduledDate, LocalDate dueDate, String status, String priority, Project project) {
		this.id = id;
		this.title = title;
		this.type = type;
		this.added_on = added_on;
		this.notes = notes;
		this.scheduledDate = scheduledDate != null ? scheduledDate : added_on;
		this.dueDate = dueDate;
		this.status = status != null ? status.toUpperCase() : "SCHEDULED";
		this.priority = priority != null ? priority.toUpperCase() : "MEDIUM";
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

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
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

	public LocalDate getScheduledDate() {
		return scheduledDate != null ? scheduledDate : added_on;
	}

	public void setScheduledDate(LocalDate scheduledDate) {
		this.scheduledDate = scheduledDate;
	}

	public LocalDate getDueDate() {
		return dueDate;
	}

	public void setDueDate(LocalDate dueDate) {
		this.dueDate = dueDate;
	}

	public String getStatus() {
		return status != null ? status : "SCHEDULED";
	}

	public void setStatus(String status) {
		this.status = status != null ? status.toUpperCase() : "SCHEDULED";
	}

	public String getPriority() {
		return priority != null ? priority : "MEDIUM";
	}

	public void setPriority(String priority) {
		this.priority = priority != null ? priority.toUpperCase() : "MEDIUM";
	}

	public LocalDate getCompletedOn() {
		return completedOn;
	}

	public void setCompletedOn(LocalDate completedOn) {
		this.completedOn = completedOn;
	}

	@com.fasterxml.jackson.annotation.JsonIgnore
	public Project getProject() {
		return project;
	}

	public void setProject(Project project) {
		this.project = project;
	}
}
