package com.smartfarm.dashboard;

import java.math.BigDecimal;
import java.util.List;

public record DashboardInventorySummaryResponse(
    SuppliesSummary supplies,
    ProduceSummary produce
) {
    public record SuppliesSummary(
        long totalItems,
        BigDecimal totalValuation,
        long inStockCount,
        long lowStockCount,
        long outOfStockCount,
        List<CategoryValuation> categories
    ) {}

    public record ProduceSummary(
        long totalProduceBatches,
        float totalProduceQuantity,
        List<ProduceStockItem> availableProduce
    ) {}

    public record CategoryValuation(
        String category,
        long itemCount,
        BigDecimal valuation
    ) {}

    public record ProduceStockItem(
        String itemName,
        float availableQuantity,
        String units,
        String projectName
    ) {}
}
