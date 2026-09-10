package com.smartfarm.customers;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerPaymentRepository extends JpaRepository<CustomerPayment, String> {

	List<CustomerPayment> findByCustomerIdOrderByPaymentDateDescCreatedAtDesc(String customerId);

	List<CustomerPayment> findBySaleIdOrderByPaymentDateDescCreatedAtDesc(String saleId);
}
