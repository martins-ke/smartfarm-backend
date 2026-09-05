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

	public Project getProject() {
		return project;
	}

	public void setProject(Project project) {
		this.project = project;
	}
}
