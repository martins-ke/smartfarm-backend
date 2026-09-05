package com.smartfarm.suppliers;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SupplierPurchaseRepository extends JpaRepository<SupplierPurchase, String> {

	List<SupplierPurchase> findBySupplierIdOrderByPurchaseDateDesc(String supplierId);

	List<SupplierPurchase> findByPaymentStatusIgnoreCase(String paymentStatus);
}
