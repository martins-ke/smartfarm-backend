package com.smartfarm.suppliers;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SupplierPaymentRepository extends JpaRepository<SupplierPayment, String> {

	List<SupplierPayment> findBySupplierIdOrderByPaymentDateDescCreatedAtDesc(String supplierId);

	List<SupplierPayment> findByPurchaseIdOrderByPaymentDateDescCreatedAtDesc(String purchaseId);
}
