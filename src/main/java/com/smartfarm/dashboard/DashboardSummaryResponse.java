package com.smartfarm.dashboard;

import java.math.BigDecimal;
import java.util.List;

public record DashboardSummaryResponse(
    Kpis kpis,
    Charts charts,
    Tables tables,
    BudgetSummary budget,
    Workforce workforce
) {
    public record Kpis(
        BigDecimal revenue,
        BigDecimal receivedRevenue,
        BigDecimal pendingDebt,
        long activeProjects,
        long lowStockCount,
        long customerCount
    ) {}

    public record Charts(
        List<SalesTrendData> salesTrend,
        List<CategoryRevenueData> revenueByCategory
    ) {}

    public record Tables(
        ProjectStatusSplit projectStatusSplit,
        List<RecentHarvest> recentHarvests,
        List<RecentSaleTransaction> recentSales,
        List<RecentSupplyTransaction> recentSupplies,
        List<RecentTransaction> recentTransactions
    ) {}

    public record SalesTrendData(
        String date,
        BigDecimal amount
    ) {}

    public record CategoryRevenueData(
        String category,
        BigDecimal revenue
    ) {}

    public record ProjectStatusSplit(
        long pending,
        long active,
        long completed
    ) {}

    public record BudgetSummary(
        BigDecimal totalBudget,
        BigDecimal totalSpent
    ) {}

    public record Workforce(
        long totalManagers,
        long totalSupervisors,
        long mySupervisors
    ) {}

    public record RecentHarvest(
        String id,
        String projectName,
        String item,
        Float quantity,
        String unit,
        String date
    ) {}

    public record RecentSaleTransaction(
        String id,
        String item,
        Float quantity,
        BigDecimal unitPrice,
        BigDecimal totalAmount,
        BigDecimal amountPaid,
        BigDecimal balanceDue,
        String paymentMode,
        String paymentStatus,
        String customerId,
        String customerName,
        String projectName,
        String date
    ) {}

    public record RecentSupplyTransaction(
        String id,
        String supplierId,
        String supplierName,
        String invoiceNumber,
        String itemName,
        BigDecimal invoiceAmount,
        BigDecimal amountPaid,
        BigDecimal balanceDue, 
        String paymentStatus,
        String purchaseDate,
        String dueDate,
        String notes
    ) {}

    public record RecentTransaction(
        String id,
        String type, // "SALE" (Inflow) or "SUPPLY" (Outflow)
        String partyId, // Customer ID or Supplier ID
        String partyName, // Customer or Supplier Name
        String description, // Item sold / bought
        BigDecimal totalAmount,
        BigDecimal amountPaid,
        BigDecimal balanceDue,
        String paymentMode, // "CASH", "MPESA", "BANK_TRANSFER", "CREDIT_LEDGER", "INVOICE"
        String paymentStatus, // "PAID", "PARTIAL", "UNPAID"
        String date,
        String reference
    ) {}
}
