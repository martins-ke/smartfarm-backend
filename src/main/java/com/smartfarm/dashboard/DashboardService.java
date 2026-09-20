package com.smartfarm.dashboard;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.smartfarm.ApiResponse;
import com.smartfarm.customers.Customer;
import com.smartfarm.customers.CustomerRepository;
import com.smartfarm.inventory.InventoryItem;
import com.smartfarm.inventory.InventoryItemRepository;
import com.smartfarm.projects.Project;
import com.smartfarm.projects.ProjectRepository;
import com.smartfarm.sales.Sale;
import com.smartfarm.sales.SalesRepository;
import com.smartfarm.expenses.Expense;
import com.smartfarm.expenses.ExpenseRepository;
import com.smartfarm.harvest.HarvestRepository;
import com.smartfarm.suppliers.Supplier;
import com.smartfarm.suppliers.SupplierPurchase;
import com.smartfarm.suppliers.SupplierPurchaseRepository;
import com.smartfarm.suppliers.SupplierRepository;
import com.smartfarm.user.UserRepository;

@Service
public class DashboardService {

    private final ProjectRepository projectRepo;
    private final SalesRepository salesRepo;
    private final InventoryItemRepository inventoryRepo;
    private final CustomerRepository customerRepo;
    private final ExpenseRepository expenseRepo;
    private final HarvestRepository harvestRepo;
    private final UserRepository userRepo;
    private final SupplierPurchaseRepository purchaseRepo;
    private final SupplierRepository supplierRepo;

    public DashboardService(ProjectRepository projectRepo, SalesRepository salesRepo,
                            InventoryItemRepository inventoryRepo, CustomerRepository customerRepo,
                            ExpenseRepository expenseRepo, HarvestRepository harvestRepo,
                            UserRepository userRepo, SupplierPurchaseRepository purchaseRepo,
                            SupplierRepository supplierRepo) {
        this.projectRepo = projectRepo;
        this.salesRepo = salesRepo;
        this.inventoryRepo = inventoryRepo;
        this.customerRepo = customerRepo;
        this.expenseRepo = expenseRepo;
        this.harvestRepo = harvestRepo;
        this.userRepo = userRepo;
        this.purchaseRepo = purchaseRepo;
        this.supplierRepo = supplierRepo;
    }

    // =========================================================================
    // 1. Immutable Scope Context (Encapsulates role access & assigned categories)
    // =========================================================================
    private record DashboardScopeContext(
            String userId,
            String userRole,
            boolean isAdmin,
            boolean isManager,
            List<Project> relevantProjects,
            Set<String> projectIds,
            Set<String> assignedCategoryIds,
            Set<String> assignedCategoryNames
    ) {}

    // =========================================================================
    // 2. Main Endpoints (Clean High-Level Orchestrations)
    // =========================================================================

    public ResponseEntity<ApiResponse<DashboardSummaryResponse>> getSummary(String userId, String userRole) {
        DashboardScopeContext ctx = resolveScopeContext(userId, userRole);
        List<Sale> relevantSales = fetchRelevantSales(ctx);
        List<SupplierPurchase> relevantPurchases = fetchRelevantPurchases(ctx);

        DashboardSummaryResponse.Kpis kpis = buildKpis(ctx, relevantSales, relevantPurchases);
        DashboardSummaryResponse.Charts charts = buildCharts(relevantSales);
        DashboardSummaryResponse.Tables tables = buildTables(ctx, relevantSales, relevantPurchases);
        DashboardSummaryResponse.BudgetSummary budget = buildBudgetSummary(ctx);
        DashboardSummaryResponse.Workforce workforce = buildWorkforce(ctx);

        DashboardSummaryResponse response = new DashboardSummaryResponse(kpis, charts, tables, budget, workforce);
        return ResponseEntity.ok(new ApiResponse<>(response, "Dashboard summary fetched successfully", true, java.time.Instant.now()));
    }

    public ResponseEntity<ApiResponse<PagedTransactionsResponse>> getTransactions(
            String userId, String userRole, int page, int size, String filter, String search) {

        DashboardScopeContext ctx = resolveScopeContext(userId, userRole);
        List<Sale> relevantSales = fetchRelevantSales(ctx);
        List<SupplierPurchase> relevantPurchases = fetchRelevantPurchases(ctx);

        List<DashboardSummaryResponse.RecentTransaction> allTransactions = new ArrayList<>();

        // 1. Process Sales Inflows
        BigDecimal totalSalesInflow = BigDecimal.ZERO;
        for (Sale s : relevantSales) {
            BigDecimal paid = s.getAmountPaid() != null ? s.getAmountPaid() : (s.getTotal_amount() != null ? s.getTotal_amount() : BigDecimal.ZERO);
            totalSalesInflow = totalSalesInflow.add(paid);
            allTransactions.add(mapSaleToRecentTransaction(s));
        }

        // 2. Process Supplies Outflows
        BigDecimal totalSuppliesOutflow = BigDecimal.ZERO;
        for (SupplierPurchase p : relevantPurchases) {
            BigDecimal paid = p.getAmountPaid() != null ? p.getAmountPaid() : (p.getInvoiceAmount() != null ? p.getInvoiceAmount() : BigDecimal.ZERO);
            totalSuppliesOutflow = totalSuppliesOutflow.add(paid);
            allTransactions.add(mapPurchaseToRecentTransaction(p));
        }

        // 3. Filter & Search
        List<DashboardSummaryResponse.RecentTransaction> filtered = filterAndSearchTransactions(allTransactions, filter, search);

        // 4. Sort Latest First
        sortTransactionsLatestFirst(filtered);

        // 5. Paginate Response
        PagedTransactionsResponse response = paginateTransactions(
                filtered,
                page,
                size,
                relevantSales.size(),
                relevantPurchases.size(),
                totalSalesInflow,
                totalSuppliesOutflow
        );

        return ResponseEntity.ok(new ApiResponse<>(response, "Transactions fetched successfully", true, java.time.Instant.now()));
    }

    // =========================================================================
    // 3. Scope & Category Access Resolvers
    // =========================================================================

    private DashboardScopeContext resolveScopeContext(String userId, String userRole) {
        boolean isAdmin = "ADMIN".equalsIgnoreCase(userRole);
        boolean isManager = "MANAGER".equalsIgnoreCase(userRole);

        List<Project> relevantProjects;
        if (isAdmin) {
            relevantProjects = projectRepo.findAll();
        } else if (isManager) {
            relevantProjects = projectRepo.findProjectsListForManager(userId);
        } else {
            relevantProjects = projectRepo.findBySupervisorId(userId);
        }

        Set<String> projectIds = relevantProjects.stream()
                .map(Project::getId)
                .collect(Collectors.toSet());

        Set<String> assignedCategoryIds = new HashSet<>();
        Set<String> assignedCategoryNames = new HashSet<>();

        if (isManager && userId != null) {
            userRepo.findById(userId).ifPresent(u -> {
                if (u.getAssignedCategories() != null) {
                    u.getAssignedCategories().forEach(c -> {
                        if (c.getId() != null) assignedCategoryIds.add(c.getId());
                        if (c.getName() != null) assignedCategoryNames.add(c.getName().trim().toLowerCase());
                    });
                }
            });
        }

        for (Project p : relevantProjects) {
            if (p.getCategory() != null) {
                if (p.getCategory().getId() != null) assignedCategoryIds.add(p.getCategory().getId());
                if (p.getCategory().getName() != null) assignedCategoryNames.add(p.getCategory().getName().trim().toLowerCase());
            }
        }

        return new DashboardScopeContext(
                userId,
                userRole,
                isAdmin,
                isManager,
                relevantProjects,
                projectIds,
                assignedCategoryIds,
                assignedCategoryNames
        );
    }

    private List<Sale> fetchRelevantSales(DashboardScopeContext ctx) {
        List<Sale> allSales = salesRepo.findAll();
        if (ctx.isAdmin()) {
            return allSales;
        }
        return allSales.stream()
                .filter(s -> s.getProject() != null && ctx.projectIds().contains(s.getProject().getId()))
                .collect(Collectors.toList());
    }

    private List<SupplierPurchase> fetchRelevantPurchases(DashboardScopeContext ctx) {
        List<SupplierPurchase> allPurchases = purchaseRepo.findAllByOrderByPurchaseDateDesc();
        if (ctx.isAdmin()) {
            return allPurchases;
        }
        if (ctx.isManager()) {
            return allPurchases.stream()
                    .filter(p -> isPurchaseInManagerScope(p, ctx))
                    .collect(Collectors.toList());
        }
        return allPurchases.stream()
                .filter(p -> p.getRecordedBy() != null && ctx.userId() != null && ctx.userId().equals(p.getRecordedBy().getId()))
                .collect(Collectors.toList());
    }

    private boolean isPurchaseInManagerScope(SupplierPurchase p, DashboardScopeContext ctx) {
        if (p.getRecordedBy() != null && ctx.userId() != null && ctx.userId().equals(p.getRecordedBy().getId())) {
            return true;
        }
        if (p.getInventoryItem() != null && p.getInventoryItem().getCategory() != null) {
            String itemCat = p.getInventoryItem().getCategory().trim();
            return ctx.assignedCategoryIds().contains(itemCat) || ctx.assignedCategoryNames().contains(itemCat.toLowerCase());
        }
        return false;
    }

    private boolean isInventoryInScope(InventoryItem item, DashboardScopeContext ctx) {
        if (ctx.isAdmin() || item.getCategory() == null) {
            return true;
        }
        String itemCat = item.getCategory().trim();
        return ctx.assignedCategoryIds().contains(itemCat) || ctx.assignedCategoryNames().contains(itemCat.toLowerCase());
    }

    // =========================================================================
    // 4. Isolated KPIs Calculator
    // =========================================================================

    private DashboardSummaryResponse.Kpis buildKpis(
            DashboardScopeContext ctx,
            List<Sale> sales,
            List<SupplierPurchase> purchases) {

        BigDecimal totalRevenue = calculateTotalRevenue(sales);
        BigDecimal pendingDebt = calculateCustomerDebt(ctx, sales);
        BigDecimal receivedRevenue = calculateReceivedRevenue(sales);
        BigDecimal supplierDebt = calculateSupplierDebt(ctx, purchases);
        long supplierDebtCount = countSupplierDebtAccounts(ctx, purchases);
        long activeProjects = countActiveProjects(ctx.relevantProjects());
        long lowStockCount = countLowStockItems(ctx);
        long customerCount = countCustomers(ctx, sales);

        return new DashboardSummaryResponse.Kpis(
                totalRevenue,
                receivedRevenue,
                pendingDebt,
                supplierDebt,
                activeProjects,
                lowStockCount,
                customerCount,
                supplierDebtCount
        );
    }

    private BigDecimal calculateTotalRevenue(List<Sale> sales) {
        return sales.stream()
                .map(Sale::getTotal_amount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateReceivedRevenue(List<Sale> sales) {
        return sales.stream()
                .map(s -> s.getAmountPaid() != null ? s.getAmountPaid() : (s.getTotal_amount() != null ? s.getTotal_amount() : BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateCustomerDebt(DashboardScopeContext ctx, List<Sale> sales) {
        if (ctx.isAdmin()) {
            return customerRepo.findAll().stream()
                    .map(Customer::getOutstandingDebt)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        return sales.stream()
                .map(Sale::getBalanceDue)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateSupplierDebt(DashboardScopeContext ctx, List<SupplierPurchase> purchases) {
        if (ctx.isAdmin()) {
            return supplierRepo.findAll().stream()
                    .map(Supplier::getBalanceOwed)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        return purchases.stream()
                .map(SupplierPurchase::getBalanceDue)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private long countSupplierDebtAccounts(DashboardScopeContext ctx, List<SupplierPurchase> purchases) {
        if (ctx.isAdmin()) {
            return supplierRepo.findAll().stream()
                    .map(Supplier::getBalanceOwed)
                    .filter(Objects::nonNull)
                    .filter(b -> b.compareTo(BigDecimal.ZERO) > 0)
                    .count();
        }
        return purchases.stream()
                .filter(p -> p.getBalanceDue() != null && p.getBalanceDue().compareTo(BigDecimal.ZERO) > 0)
                .map(p -> p.getSupplier() != null ? p.getSupplier().getId() : p.getId())
                .distinct()
                .count();
    }

    private long countActiveProjects(List<Project> projects) {
        return projects.stream()
                .filter(p -> !"COMPLETED".equalsIgnoreCase(p.getStatus()) && !"DONE".equalsIgnoreCase(p.getStatus()))
                .count();
    }

    private long countLowStockItems(DashboardScopeContext ctx) {
        return inventoryRepo.findAll().stream()
                .filter(item -> isInventoryInScope(item, ctx))
                .filter(item -> item.getQuantityInStock() != null && item.getMinStockLevel() != null)
                .filter(item -> item.getQuantityInStock().compareTo(item.getMinStockLevel()) <= 0)
                .count();
    }

    private long countCustomers(DashboardScopeContext ctx, List<Sale> sales) {
        if (ctx.isAdmin()) {
            return customerRepo.count();
        }
        return sales.stream()
                .map(Sale::getCustomer)
                .filter(Objects::nonNull)
                .map(Customer::getId)
                .distinct()
                .count();
    }

    // =========================================================================
    // 5. Isolated Charts Builder
    // =========================================================================

    private DashboardSummaryResponse.Charts buildCharts(List<Sale> sales) {
        List<DashboardSummaryResponse.CategoryRevenueData> revenueByCategory = buildCategoryRevenue(sales);
        List<DashboardSummaryResponse.SalesTrendData> salesTrend = buildSalesTrend(sales, 10);
        return new DashboardSummaryResponse.Charts(salesTrend, revenueByCategory);
    }

    private List<DashboardSummaryResponse.CategoryRevenueData> buildCategoryRevenue(List<Sale> sales) {
        Map<String, BigDecimal> map = new HashMap<>();
        for (Sale s : sales) {
            if (s.getProject() != null && s.getProject().getCategory() != null) {
                String catName = s.getProject().getCategory().getName();
                BigDecimal amount = s.getTotal_amount() != null ? s.getTotal_amount() : BigDecimal.ZERO;
                map.put(catName, map.getOrDefault(catName, BigDecimal.ZERO).add(amount));
            }
        }
        return map.entrySet().stream()
                .map(e -> new DashboardSummaryResponse.CategoryRevenueData(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    private List<DashboardSummaryResponse.SalesTrendData> buildSalesTrend(List<Sale> sales, int maxDays) {
        Map<String, BigDecimal> map = new TreeMap<>();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (Sale s : sales) {
            if (s.getAddedOn() != null) {
                String dateStr = s.getAddedOn().format(dtf);
                BigDecimal amount = s.getTotal_amount() != null ? s.getTotal_amount() : BigDecimal.ZERO;
                map.put(dateStr, map.getOrDefault(dateStr, BigDecimal.ZERO).add(amount));
            }
        }

        return map.entrySet().stream()
                .skip(Math.max(0, map.size() - maxDays))
                .map(e -> new DashboardSummaryResponse.SalesTrendData(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    // =========================================================================
    // 6. Isolated Tables & History Feeds Builder
    // =========================================================================

    private DashboardSummaryResponse.Tables buildTables(
            DashboardScopeContext ctx,
            List<Sale> sales,
            List<SupplierPurchase> purchases) {

        DashboardSummaryResponse.ProjectStatusSplit statusSplit = buildProjectStatusSplit(ctx.relevantProjects());
        List<DashboardSummaryResponse.RecentHarvest> recentHarvests = buildRecentHarvests(ctx.projectIds(), 5);
        List<DashboardSummaryResponse.RecentSaleTransaction> recentSales = buildRecentSales(sales, 10);
        List<DashboardSummaryResponse.RecentSupplyTransaction> recentSupplies = buildRecentSupplies(purchases, 10);
        List<DashboardSummaryResponse.RecentTransaction> recentTransactions = buildCombinedTransactions(recentSales, recentSupplies);

        return new DashboardSummaryResponse.Tables(
                statusSplit,
                recentHarvests,
                recentSales,
                recentSupplies,
                recentTransactions
        );
    }

    private DashboardSummaryResponse.ProjectStatusSplit buildProjectStatusSplit(List<Project> projects) {
        long active = 0;
        long completed = 0;
        for (Project p : projects) {
            if ("COMPLETED".equalsIgnoreCase(p.getStatus()) || "DONE".equalsIgnoreCase(p.getStatus())) {
                completed++;
            } else {
                active++;
            }
        }
        return new DashboardSummaryResponse.ProjectStatusSplit(0, active, completed);
    }

    private List<DashboardSummaryResponse.RecentHarvest> buildRecentHarvests(Set<String> projectIds, int limit) {
        return harvestRepo.findAll().stream()
                .filter(h -> h.getProject() != null && projectIds.contains(h.getProject().getId()))
                .sorted((h1, h2) -> {
                    if (h1.getAdded_on() == null) return 1;
                    if (h2.getAdded_on() == null) return -1;
                    return h2.getAdded_on().compareTo(h1.getAdded_on());
                })
                .limit(limit)
                .map(h -> new DashboardSummaryResponse.RecentHarvest(
                        h.getId(),
                        h.getProject().getName(),
                        h.getItem(),
                        h.getQuantity(),
                        h.getUnits(),
                        h.getAdded_on() != null ? h.getAdded_on().toString() : "N/A"
                ))
                .collect(Collectors.toList());
    }

    private List<DashboardSummaryResponse.RecentSaleTransaction> buildRecentSales(List<Sale> sales, int limit) {
        return sales.stream()
                .sorted((s1, s2) -> {
                    if (s1.getAddedOn() == null) return 1;
                    if (s2.getAddedOn() == null) return -1;
                    return s2.getAddedOn().compareTo(s1.getAddedOn());
                })
                .limit(limit)
                .map(s -> new DashboardSummaryResponse.RecentSaleTransaction(
                        s.getId(),
                        s.getItem(),
                        s.getQuantity(),
                        s.getUnit_price(),
                        s.getTotal_amount(),
                        s.getAmountPaid(),
                        s.getBalanceDue(),
                        s.getPaymentMode(),
                        s.getPaymentStatus(),
                        s.getCustomer() != null ? s.getCustomer().getId() : null,
                        s.getCustomer() != null ? s.getCustomer().getName() : "Walk-in Buyer",
                        s.getProject() != null ? s.getProject().getName() : "General",
                        s.getAddedOn() != null ? s.getAddedOn().toString() : ""
                ))
                .collect(Collectors.toList());
    }

    private List<DashboardSummaryResponse.RecentSupplyTransaction> buildRecentSupplies(List<SupplierPurchase> purchases, int limit) {
        return purchases.stream()
                .limit(limit)
                .map(p -> new DashboardSummaryResponse.RecentSupplyTransaction(
                        p.getId(),
                        p.getSupplier() != null ? p.getSupplier().getId() : null,
                        p.getSupplier() != null ? p.getSupplier().getName() : "Supplier",
                        p.getInvoiceNumber(),
                        p.getInventoryItem() != null ? p.getInventoryItem().getName() : (p.getNotes() != null ? p.getNotes() : "Farm Supplies"),
                        p.getInvoiceAmount(),
                        p.getAmountPaid(),
                        p.getBalanceDue(),
                        p.getPaymentStatus(),
                        p.getPurchaseDate() != null ? p.getPurchaseDate().toString() : "",
                        p.getDueDate() != null ? p.getDueDate().toString() : "",
                        p.getNotes()
                ))
                .collect(Collectors.toList());
    }

    private List<DashboardSummaryResponse.RecentTransaction> buildCombinedTransactions(
            List<DashboardSummaryResponse.RecentSaleTransaction> sales,
            List<DashboardSummaryResponse.RecentSupplyTransaction> supplies) {

        List<DashboardSummaryResponse.RecentTransaction> list = new ArrayList<>();

        for (DashboardSummaryResponse.RecentSaleTransaction s : sales) {
            String status = resolveSaleDisplayStatus(s.paymentStatus(), s.balanceDue());
            list.add(new DashboardSummaryResponse.RecentTransaction(
                    s.id(),
                    "SALE",
                    s.customerId(),
                    s.customerName(),
                    s.item() + (s.quantity() != null ? " (" + s.quantity() + " units)" : ""),
                    s.totalAmount(),
                    s.amountPaid(),
                    s.balanceDue(),
                    s.paymentMode() != null ? s.paymentMode() : "CASH",
                    status,
                    s.date(),
                    s.id()
            ));
        }

        for (DashboardSummaryResponse.RecentSupplyTransaction p : supplies) {
            list.add(new DashboardSummaryResponse.RecentTransaction(
                    p.id(),
                    "SUPPLY",
                    p.supplierId(),
                    p.supplierName(),
                    p.itemName() != null ? p.itemName() : (p.notes() != null ? p.notes() : "Supplies"),
                    p.invoiceAmount(),
                    p.amountPaid(),
                    p.balanceDue(),
                    "INVOICE",
                    p.paymentStatus() != null ? p.paymentStatus() : "PAID",
                    p.purchaseDate(),
                    p.invoiceNumber() != null ? p.invoiceNumber() : p.id()
            ));
        }

        sortTransactionsLatestFirst(list);
        return list;
    }

    // =========================================================================
    // 7. Budget & Workforce Aggregators
    // =========================================================================

    private DashboardSummaryResponse.BudgetSummary buildBudgetSummary(DashboardScopeContext ctx) {
        BigDecimal totalBudget = ctx.relevantProjects().stream()
                .map(Project::getBudget)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalSpent = ctx.projectIds().stream()
                .map(expenseRepo::findByProjectId)
                .flatMap(List::stream)
                .map(Expense::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new DashboardSummaryResponse.BudgetSummary(totalBudget, totalSpent);
    }

    private DashboardSummaryResponse.Workforce buildWorkforce(DashboardScopeContext ctx) {
        long totalManagers = userRepo.countByRoleIgnoreCase("MANAGER");
        long totalSupervisors = userRepo.countByRoleIgnoreCase("SUPERVISOR");
        long mySupervisors = ctx.isManager() ? userRepo.countByCreatedById(ctx.userId()) : 0;
        return new DashboardSummaryResponse.Workforce(totalManagers, totalSupervisors, mySupervisors);
    }

    // =========================================================================
    // 8. Transaction Mapping & Pagination Helpers
    // =========================================================================

    private DashboardSummaryResponse.RecentTransaction mapSaleToRecentTransaction(Sale s) {
        String status = resolveSaleDisplayStatus(s.getPaymentStatus(), s.getBalanceDue());
        String cId = s.getCustomer() != null ? s.getCustomer().getId() : "";
        String cName = s.getCustomer() != null ? s.getCustomer().getName() : "Direct Buyer";

        return new DashboardSummaryResponse.RecentTransaction(
                s.getId(),
                "SALE",
                cId,
                cName,
                (s.getItem() != null ? s.getItem() : "Produce") + " (" + s.getQuantity() + " units)",
                s.getTotal_amount(),
                s.getAmountPaid(),
                s.getBalanceDue(),
                s.getPaymentMode() != null ? s.getPaymentMode() : "CASH",
                status,
                s.getAddedOn() != null ? s.getAddedOn().toString() : "",
                s.getId()
        );
    }

    private DashboardSummaryResponse.RecentTransaction mapPurchaseToRecentTransaction(SupplierPurchase p) {
        String supId = p.getSupplier() != null ? p.getSupplier().getId() : "";
        String supName = p.getSupplier() != null ? p.getSupplier().getName() : "Direct Supplier";
        String itemDesc = p.getInventoryItem() != null ? p.getInventoryItem().getName() : (p.getNotes() != null ? p.getNotes() : "Supplies");

        return new DashboardSummaryResponse.RecentTransaction(
                p.getId(),
                "SUPPLY",
                supId,
                supName,
                itemDesc,
                p.getInvoiceAmount(),
                p.getAmountPaid(),
                p.getBalanceDue(),
                "INVOICE",
                p.getPaymentStatus() != null ? p.getPaymentStatus() : "PAID",
                p.getPurchaseDate() != null ? p.getPurchaseDate().toString() : "",
                p.getInvoiceNumber() != null ? p.getInvoiceNumber() : p.getId()
        );
    }

    private String resolveSaleDisplayStatus(String paymentStatus, BigDecimal balanceDue) {
        if ("CREDIT_UNPAID".equalsIgnoreCase(paymentStatus)) {
            return "UNPAID";
        }
        if ("PARTIAL_PAYMENT".equalsIgnoreCase(paymentStatus) || (balanceDue != null && balanceDue.compareTo(BigDecimal.ZERO) > 0)) {
            return "PARTIAL";
        }
        return "PAID";
    }

    private List<DashboardSummaryResponse.RecentTransaction> filterAndSearchTransactions(
            List<DashboardSummaryResponse.RecentTransaction> list,
            String filter,
            String search) {

        String normFilter = filter != null ? filter.trim().toUpperCase() : "ALL";
        List<DashboardSummaryResponse.RecentTransaction> filtered = list.stream()
                .filter(t -> {
                    if ("SALES".equals(normFilter) && !"SALE".equals(t.type())) return false;
                    if ("SUPPLIES".equals(normFilter) && !"SUPPLY".equals(t.type())) return false;
                    return true;
                })
                .collect(Collectors.toList());

        if (search != null && !search.trim().isEmpty()) {
            String q = search.trim().toLowerCase();
            filtered = filtered.stream()
                    .filter(t -> {
                        String party = t.partyName() != null ? t.partyName().toLowerCase() : "";
                        String desc = t.description() != null ? t.description().toLowerCase() : "";
                        String ref = t.reference() != null ? t.reference().toLowerCase() : "";
                        String id = t.id() != null ? t.id().toLowerCase() : "";
                        String status = t.paymentStatus() != null ? t.paymentStatus().toLowerCase() : "";
                        String mode = t.paymentMode() != null ? t.paymentMode().toLowerCase() : "";
                        return party.contains(q) || desc.contains(q) || ref.contains(q) || id.contains(q) || status.contains(q) || mode.contains(q);
                    })
                    .collect(Collectors.toList());
        }

        return filtered;
    }

    private void sortTransactionsLatestFirst(List<DashboardSummaryResponse.RecentTransaction> list) {
        list.sort((t1, t2) -> {
            String d1 = t1.date() != null ? t1.date() : "";
            String d2 = t2.date() != null ? t2.date() : "";
            int cmp = d2.compareTo(d1);
            if (cmp != 0) return cmp;
            String id1 = t1.id() != null ? t1.id() : "";
            String id2 = t2.id() != null ? t2.id() : "";
            return id2.compareTo(id1);
        });
    }

    private PagedTransactionsResponse paginateTransactions(
            List<DashboardSummaryResponse.RecentTransaction> filtered,
            int page,
            int size,
            long totalSalesCount,
            long totalSuppliesCount,
            BigDecimal totalSalesInflow,
            BigDecimal totalSuppliesOutflow) {

        int effectivePage = page <= 0 ? 1 : page;
        int pageSize = size <= 0 ? 5 : size;
        int totalElements = filtered.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalElements / pageSize));

        int fromIndex = (effectivePage - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, totalElements);

        List<DashboardSummaryResponse.RecentTransaction> pagedContent = Collections.emptyList();
        if (fromIndex < totalElements) {
            pagedContent = new ArrayList<>(filtered.subList(fromIndex, toIndex));
        }

        boolean hasNext = effectivePage < totalPages;
        boolean hasPrevious = effectivePage > 1;

        return new PagedTransactionsResponse(
                pagedContent,
                effectivePage,
                pageSize,
                totalElements,
                totalPages,
                hasNext,
                hasPrevious,
                totalSalesCount,
                totalSuppliesCount,
                totalSalesInflow,
                totalSuppliesOutflow
        );
    }
}
