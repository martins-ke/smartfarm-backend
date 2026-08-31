package com.smartfarm.customers;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.smartfarm.sales.Sale;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table
public class Customer {

	@Id
	private String id;
	private String name;
	@Column(unique = true)
	private String contact;
	@Column(unique = true) 
	@JsonProperty("id_number")
	private String idNumber;
	private String address;
	private boolean isActive;
	
	@OneToMany(mappedBy = "customer")
	@JsonIgnore
	private List<Sale> sales;
	
	public Customer() {}

	public Customer(String id, String name, String contact, String idNumber, String address, boolean isActive) {
		super();
		this.id = id;
		this.name = name;
		this.contact = contact;
		this.idNumber = idNumber;
		this.address = address;
		this.isActive = isActive;
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

	public String getContact() {
		return contact;
	}

	public void setContact(String contact) {
		this.contact = contact;
	}

	public String getIdNumber() {
		return idNumber;
	}

	public void setIdNumber(String idNumber) {
		this.idNumber = idNumber;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public boolean isActive() {
		return isActive;
	}

	public void setActive(boolean isActive) {
		this.isActive = isActive;
	}
	
}
