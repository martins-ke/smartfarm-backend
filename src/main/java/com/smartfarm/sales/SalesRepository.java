package com.smartfarm.sales;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SalesRepository extends JpaRepository<Sale, String> {

	@Query("SELECT s FROM Sale s WHERE s.project.id = :projectId")
	List<Sale> findByProjectId(@Param("projectId") String projectId);
	Page<Sale> findAll(Pageable pageable);
	List<Sale> findAllByOrderByAddedOnDesc();
	List<Sale> findTop10ByOrderByAddedOnDesc();
	@Query("SELECT s FROM Sale s WHERE s.project.id = :projectId")
	Page<Sale> findByProjectId(@Param("projectId") String projectId, Pageable pageable);
	List<Sale> findByCustomerId(String customerId);
	List<Sale> findByCustomerIdOrderByAddedOnAsc(String customerId);
	Page<Sale> findByCustomerId(String customerId, Pageable pageable);

	@Query("SELECT COALESCE(SUM(s.total_amount), 0) FROM Sale s WHERE s.project.id = :projectId")
	BigDecimal totalSalesByProjectId(@Param("projectId") String projectId);

	@Query("SELECT COALESCE(SUM(s.quantity), 0) FROM Sale s WHERE s.project.id = :projectId")
	Float totalSoldQuantityByProjectId(@Param("projectId") String projectId);

	@Query("SELECT COALESCE(SUM(s.quantity), 0) FROM Sale s WHERE s.project.id = :projectId AND LOWER(TRIM(s.item)) = LOWER(TRIM(:item))")
	Float totalSoldQuantityByProjectIdAndItem(@Param("projectId") String projectId, @Param("item") String item);

	@Query("SELECT COALESCE(SUM(s.total_amount), 0) FROM Sale s")
	BigDecimal totalAllSales();

	@Query("SELECT COALESCE(SUM(s.total_amount), 0) FROM Sale s WHERE s.project.id IN :projectIds")
	BigDecimal totalSalesByProjectIds(@Param("projectIds") java.util.Collection<String> projectIds);

	@Query("SELECT COALESCE(SUM(CASE WHEN s.amountPaid IS NOT NULL THEN s.amountPaid ELSE s.total_amount END), 0) FROM Sale s")
	BigDecimal totalAllReceivedSalesRevenue();

	@Query("SELECT COALESCE(SUM(CASE WHEN s.amountPaid IS NOT NULL THEN s.amountPaid ELSE s.total_amount END), 0) FROM Sale s WHERE s.project.id IN :projectIds")
	BigDecimal totalReceivedSalesRevenueByProjectIds(@Param("projectIds") java.util.Collection<String> projectIds);

	@Query("SELECT s FROM Sale s WHERE s.project.id IN :projectIds")
	List<Sale> findByProjectIdIn(@Param("projectIds") java.util.Collection<String> projectIds);

	@Query("SELECT MIN(YEAR(s.addedOn)) FROM Sale s WHERE s.addedOn IS NOT NULL")
	Integer findEarliestSaleYear();

	@Query("SELECT COALESCE(SUM(s.total_amount), 0) FROM Sale s WHERE s.addedOn BETWEEN :startDate AND :endDate")
	BigDecimal totalSalesBetween(@Param("startDate") java.time.LocalDate startDate, @Param("endDate") java.time.LocalDate endDate);

	@Query("SELECT COALESCE(SUM(s.total_amount), 0) FROM Sale s WHERE s.project.id IN :projectIds AND s.addedOn BETWEEN :startDate AND :endDate")
	BigDecimal totalSalesByProjectIdsBetween(@Param("projectIds") java.util.Collection<String> projectIds, @Param("startDate") java.time.LocalDate startDate, @Param("endDate") java.time.LocalDate endDate);

	@Query("SELECT COALESCE(SUM(CASE WHEN s.amountPaid IS NOT NULL THEN s.amountPaid ELSE s.total_amount END), 0) FROM Sale s WHERE s.addedOn BETWEEN :startDate AND :endDate")
	BigDecimal totalReceivedSalesRevenueBetween(@Param("startDate") java.time.LocalDate startDate, @Param("endDate") java.time.LocalDate endDate);

	@Query("SELECT COALESCE(SUM(CASE WHEN s.amountPaid IS NOT NULL THEN s.amountPaid ELSE s.total_amount END), 0) FROM Sale s WHERE s.project.id IN :projectIds AND s.addedOn BETWEEN :startDate AND :endDate")
	BigDecimal totalReceivedSalesRevenueByProjectIdsBetween(@Param("projectIds") java.util.Collection<String> projectIds, @Param("startDate") java.time.LocalDate startDate, @Param("endDate") java.time.LocalDate endDate);

	@Query("SELECT s FROM Sale s WHERE s.addedOn BETWEEN :startDate AND :endDate ORDER BY s.addedOn DESC")
	List<Sale> findAllBetween(@Param("startDate") java.time.LocalDate startDate, @Param("endDate") java.time.LocalDate endDate);

	@Query("SELECT s FROM Sale s WHERE s.project.id IN :projectIds AND s.addedOn BETWEEN :startDate AND :endDate ORDER BY s.addedOn DESC")
	List<Sale> findByProjectIdInBetween(@Param("projectIds") java.util.Collection<String> projectIds, @Param("startDate") java.time.LocalDate startDate, @Param("endDate") java.time.LocalDate endDate);
}
