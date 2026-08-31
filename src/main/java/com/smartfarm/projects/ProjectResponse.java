package com.smartfarm.projects;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.smartfarm.activities.Activity;
import com.smartfarm.expenses.ExpenseResponse;
import com.smartfarm.harvest.Harvest;
import com.smartfarm.sales.Sale;

public record ProjectResponse(
		String id,
		String name,
		String season,
		BigDecimal budget,
		String status,
		LocalDate startDate,
		LocalDate endDate,
		String description,
		BigDecimal totalSales,
		BigDecimal totalExpenses,
		BigDecimal netValue,
		List<ExpenseResponse> expenses,
		List<Sale> sales,
		List<Harvest> harvest,
		List<Activity> activities
		) {

}
