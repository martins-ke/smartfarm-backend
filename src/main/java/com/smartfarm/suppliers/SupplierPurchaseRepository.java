package com.smartfarm.suppliers;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface SupplierPurchaseRepository extends JpaRepository<SupplierPurchase, String> {

	List<SupplierPurchase> findBySupplierIdOrderByPurchaseDateDesc(String supplierId);
	List<SupplierPurchase> findBySupplierIdOrderByPurchaseDateAsc(String supplierId);

	List<SupplierPurchase> findAllByOrderByPurchaseDateDesc();

	List<SupplierPurchase> findTop10ByOrderByPurchaseDateDesc();

	List<SupplierPurchase> findByPaymentStatusIgnoreCase(String paymentStatus);

	List<SupplierPurchase> findByRecordedByIdOrderByPurchaseDateDesc(String recordedById);
	List<SupplierPurchase> findByRecordedById(String recordedById);

	@Query("SELECT COALESCE(SUM(p.invoiceAmount), 0) FROM SupplierPurchase p")
	BigDecimal totalAllPurchaseInvoices();

	@Query("SELECT COALESCE(SUM(CASE WHEN p.amountPaid IS NOT NULL THEN p.amountPaid ELSE p.invoiceAmount END), 0) FROM SupplierPurchase p")
	BigDecimal totalAllPurchasePaid();

	@Query("SELECT MIN(YEAR(p.purchaseDate)) FROM SupplierPurchase p WHERE p.purchaseDate IS NOT NULL")
	Integer findEarliestPurchaseYear();

	@Query("SELECT COALESCE(SUM(CASE WHEN p.amountPaid IS NOT NULL THEN p.amountPaid ELSE p.invoiceAmount END), 0) FROM SupplierPurchase p WHERE p.purchaseDate BETWEEN :startDate AND :endDate")
	BigDecimal totalPurchasePaidBetween(@org.springframework.data.repository.query.Param("startDate") java.time.LocalDate startDate, @org.springframework.data.repository.query.Param("endDate") java.time.LocalDate endDate);

	@Query("SELECT p FROM SupplierPurchase p WHERE p.purchaseDate BETWEEN :startDate AND :endDate ORDER BY p.purchaseDate DESC")
	List<SupplierPurchase> findAllBetween(@org.springframework.data.repository.query.Param("startDate") java.time.LocalDate startDate, @org.springframework.data.repository.query.Param("endDate") java.time.LocalDate endDate);
}
