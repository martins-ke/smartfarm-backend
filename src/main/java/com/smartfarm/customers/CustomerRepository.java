package com.smartfarm.customers;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CustomerRepository extends JpaRepository<Customer, String> {

	boolean existsByidNumber(String idNumber);
	boolean existsByIdNumber(String idNumber);
	boolean existsByContact(String contact);
	Optional<Customer> findByContact(String contact);
	Optional<Customer> findByIdNumber(String idNumber);

	@Query("SELECT COALESCE(SUM(c.outstandingDebt), 0) FROM Customer c")
	BigDecimal totalOutstandingDebt();
}
