package com.smartfarm.sales;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SalesRepository extends JpaRepository<Sale, String> {

	List<Sale> findByProjectId(String projectId);
	Page<Sale> findAll(Pageable pageable);
	Page<Sale> findByProjectId(String projectId, Pageable pageable);
	List<Sale> findByCustomerId(String customerId);

	@Query("SELECT COALESCE(SUM(s.total_amount), 0) FROM Sale s WHERE s.project.id = :projectId")
	BigDecimal totalSalesByProjectId(@Param("projectId") String projectId);

	@Query("SELECT COALESCE(SUM(s.quantity), 0) FROM Sale s WHERE s.project.id = :projectId")
	Float totalSoldQuantityByProjectId(@Param("projectId") String projectId);

	@Query("SELECT COALESCE(SUM(s.quantity), 0) FROM Sale s WHERE s.project.id = :projectId AND LOWER(TRIM(s.item)) = LOWER(TRIM(:item))")
	Float totalSoldQuantityByProjectIdAndItem(@Param("projectId") String projectId, @Param("item") String item);
}
