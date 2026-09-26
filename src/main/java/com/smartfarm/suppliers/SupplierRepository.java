package com.smartfarm.suppliers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, String> {

	boolean existsByNameIgnoreCase(String name);

	Optional<Supplier> findByNameIgnoreCase(String name);

	List<Supplier> findByIsActiveTrue();

	@Query("SELECT COALESCE(SUM(s.balanceOwed), 0) FROM Supplier s WHERE s.isActive = true")
	BigDecimal totalBalanceOwed();

	@Query("SELECT COUNT(s) FROM Supplier s WHERE s.isActive = true AND s.balanceOwed > 0")
	long countSuppliersWithDebt();
}
