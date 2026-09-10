package com.smartfarm.dashboard;

import java.math.BigDecimal;
import java.util.List;

public record PagedTransactionsResponse(
    List<DashboardSummaryResponse.RecentTransaction> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean hasNext,
    boolean hasPrevious,
    long totalSalesCount,
    long totalSuppliesCount,
    BigDecimal totalSalesInflow,
    BigDecimal totalSuppliesOutflow
) {}
