package com.smartfarm.harvest;

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
@Table(name = "harvest")
public class Harvest {

	@Id
	private String id;
	private String item;
	@Column(nullable = false)
	private float quantity;
	private String units;
	private String notes;
	@CreationTimestamp
	private LocalDate added_on;
	@ManyToOne
	@JoinColumn(name = "project_id")
	private Project project;
	
	public Harvest() {}
	
	public Harvest(String id, String item, float quantity, String units, String notes,LocalDate added_on, Project project) {
		super();
		this.id = id;
		this.item = item;
		this.quantity = quantity;
		this.units = units;
		this.notes = notes;
		this.added_on = added_on;
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
	public String getUnits() {
		return units;
	}
	public void setUnits(String units) {
		this.units = units;
	}
	public String getNotes() {
		return notes;
	}
	public void setNotes(String notes) {
		this.notes = notes;
	}
	
	public LocalDate getAdded_on() {
		return added_on;
	}

	public void setAdded_on(LocalDate added_on) {
		this.added_on = added_on;
	}

	public Project getProject() {
		return project;
	}
	public void setProject(Project project) {
		this.project = project;
	}
	
}
