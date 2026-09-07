package com.smartfarm.dashboard;

import java.math.BigDecimal;
import java.util.List;

public record DashboardSummaryResponse(
    Kpis kpis,
    Charts charts,
    Tables tables
) {
    public record Kpis(
        BigDecimal revenue,
        long activeProjects,
        long lowStockCount,
        long customerCount
    ) {}

    public record Charts(
        List<SalesTrendData> salesTrend,
        List<CategoryRevenueData> revenueByCategory
    ) {}

    public record Tables(
        List<LowStockItem> lowStockItems,
        ProjectStatusSplit projectStatusSplit
    ) {}

    public record SalesTrendData(
        String date,
        BigDecimal amount
    ) {}

    public record CategoryRevenueData(
        String category,
        BigDecimal revenue
    ) {}

    public record LowStockItem(
        String id,
        String name,
        String category,
        Float currentQuantity,
        Float threshold
    ) {}

    public record ProjectStatusSplit(
        long pending,
        long active,
        long completed
    ) {}
}
