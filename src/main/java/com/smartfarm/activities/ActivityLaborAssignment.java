package com.smartfarm.activities;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.smartfarm.employees.Employee;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "activity_labor_assignments")
public class ActivityLaborAssignment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "activity_id", nullable = false)
	@JsonIgnore
	private Activity activity;

	@ManyToOne
	@JoinColumn(name = "employee_id", nullable = false)
	private Employee employee;

	private LocalDate assignmentDate;

	private double hoursWorked;

	private BigDecimal wagePayable;

	private String notes;

	public ActivityLaborAssignment() {}

	public ActivityLaborAssignment(Activity activity, Employee employee, LocalDate assignmentDate, double hoursWorked,
			BigDecimal wagePayable, String notes) {
		this.activity = activity;
		this.employee = employee;
		this.assignmentDate = assignmentDate != null ? assignmentDate : LocalDate.now();
		this.hoursWorked = hoursWorked;
		this.wagePayable = wagePayable;
		this.notes = notes;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Activity getActivity() {
		return activity;
	}

	public void setActivity(Activity activity) {
		this.activity = activity;
	}

	public Employee getEmployee() {
		return employee;
	}

	public void setEmployee(Employee employee) {
		this.employee = employee;
	}

	public LocalDate getAssignmentDate() {
		return assignmentDate;
	}

	public void setAssignmentDate(LocalDate assignmentDate) {
		this.assignmentDate = assignmentDate;
	}

	public double getHoursWorked() {
		return hoursWorked;
	}

	public void setHoursWorked(double hoursWorked) {
		this.hoursWorked = hoursWorked;
	}

	public BigDecimal getWagePayable() {
		return wagePayable;
	}

	public void setWagePayable(BigDecimal wagePayable) {
		this.wagePayable = wagePayable;
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}
}
