package com.smartfarm.expenses;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExpenseRepository extends JpaRepository<Expense, String> {

	@Query("SELECT e FROM Expense e WHERE e.project.id = :projectId")
	List<Expense> findByProjectId(@Param("projectId") String projectId);

	@Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.project.id = :projectId")
	BigDecimal totalExpensesByProjectId(@Param("projectId") String projectId);

	@Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e")
	BigDecimal totalAllExpenses();

	@Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.project.id IN :projectIds")
	BigDecimal totalExpensesByProjectIds(@Param("projectIds") java.util.Collection<String> projectIds);

	@Query("SELECT MIN(YEAR(e.added_on)) FROM Expense e WHERE e.added_on IS NOT NULL")
	Integer findEarliestExpenseYear();

	@Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.added_on BETWEEN :startDate AND :endDate")
	BigDecimal totalExpensesBetween(@Param("startDate") java.time.LocalDate startDate, @Param("endDate") java.time.LocalDate endDate);

	@Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.project.id IN :projectIds AND e.added_on BETWEEN :startDate AND :endDate")
	BigDecimal totalExpensesByProjectIdsBetween(@Param("projectIds") java.util.Collection<String> projectIds, @Param("startDate") java.time.LocalDate startDate, @Param("endDate") java.time.LocalDate endDate);
}
