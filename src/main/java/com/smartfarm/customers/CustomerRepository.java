package com.smartfarm.customers;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, String> {

	boolean existsByidNumber(String idNumber);
	boolean existsByIdNumber(String idNumber);
	boolean existsByContact(String contact);
	Optional<Customer> findByContact(String contact);
	Optional<Customer> findByIdNumber(String idNumber);
}
