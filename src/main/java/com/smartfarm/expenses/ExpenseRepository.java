package com.smartfarm.expenses;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExpenseRepository extends JpaRepository<Expense, String> {

	List<Expense> findByProjectId(String project_id);

	@Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.project.id = :projectId")
	BigDecimal totalExpensesByProjectId(@Param("projectId") String projectId);
}
