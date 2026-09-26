package com.smartfarm.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.smartfarm.ApiResponse;
import com.smartfarm.activities.Activity;
import com.smartfarm.activities.ActivityLaborAssignmentRepository;
import com.smartfarm.activities.ActivityRepository;
import com.smartfarm.customers.CustomerRepository;
import com.smartfarm.inventory.InventoryItem;
import com.smartfarm.inventory.InventoryItemRepository;
import com.smartfarm.projects.Project;
import com.smartfarm.projects.ProjectRepository;
import com.smartfarm.sales.Sale;
import com.smartfarm.sales.SalesRepository;
import com.smartfarm.expenses.ExpenseRepository;
import com.smartfarm.harvest.Harvest;
import com.smartfarm.harvest.HarvestInventory;
import com.smartfarm.harvest.HarvestInventoryRepository;
import com.smartfarm.harvest.HarvestRepository;
import com.smartfarm.suppliers.SupplierPurchase;
import com.smartfarm.suppliers.SupplierPurchaseRepository;
import com.smartfarm.suppliers.SupplierRepository;
import com.smartfarm.user.User;
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
    private final HarvestInventoryRepository harvestInventoryRepo;
    private final ActivityRepository activityRepo;
    private final ActivityLaborAssignmentRepository laborRepo;

    public DashboardService(ProjectRepository projectRepo, SalesRepository salesRepo,
                            InventoryItemRepository inventoryRepo, CustomerRepository customerRepo,
                            ExpenseRepository expenseRepo, HarvestRepository harvestRepo,
                            UserRepository userRepo, SupplierPurchaseRepository purchaseRepo,
                            SupplierRepository supplierRepo, HarvestInventoryRepository harvestInventoryRepo,
                            ActivityRepository activityRepo, ActivityLaborAssignmentRepository laborRepo) {
        this.projectRepo = projectRepo;
        this.salesRepo = salesRepo;
        this.inventoryRepo = inventoryRepo;
        this.customerRepo = customerRepo;
        this.expenseRepo = expenseRepo;
        this.harvestRepo = harvestRepo;
        this.userRepo = userRepo;
        this.purchaseRepo = purchaseRepo;
        this.supplierRepo = supplierRepo;
        this.harvestInventoryRepo = harvestInventoryRepo;
        this.activityRepo = activityRepo;
        this.laborRepo = laborRepo;
    }

    // =========================================================================
    // 1. Immutable Scope Context (Encapsulates role access & assigned categories)
    // =========================================================================
    private record DashboardScopeContext(
            User currentUser,
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

    public ResponseEntity<ApiResponse<DashboardSummaryResponse>> getSummary(User currentUser) {
        return getSummary(currentUser, null);
    }

    public ResponseEntity<ApiResponse<DashboardSummaryResponse>> getSummary(User currentUser, String yearParam) {
        int currentYear = LocalDate.now().getYear();
        List<String> availableYears = resolveAvailableYears(currentYear);
        String selectedYear = resolveSelectedYear(yearParam, currentYear, availableYears);

        LocalDate startDate = null;
        LocalDate endDate = null;
        if (!"ALL".equalsIgnoreCase(selectedYear)) {
            try {
                int y = Integer.parseInt(selectedYear);
                startDate = LocalDate.of(y, 1, 1);
                endDate = LocalDate.of(y, 12, 31);
            } catch (NumberFormatException ignored) {
                selectedYear = String.valueOf(currentYear);
                startDate = LocalDate.of(currentYear, 1, 1);
                endDate = LocalDate.of(currentYear, 12, 31);
            }
        }

        DashboardScopeContext ctx = resolveScopeContext(currentUser, startDate, endDate);
        List<Sale> relevantSales = fetchRelevantSales(ctx, startDate, endDate);
        List<SupplierPurchase> relevantPurchases = fetchRelevantPurchases(ctx, startDate, endDate);

        DashboardSummaryResponse.Kpis kpis = buildKpis(ctx, relevantSales, relevantPurchases, startDate, endDate);
        DashboardSummaryResponse.Charts charts = buildCharts(relevantSales);
        DashboardSummaryResponse.Tables tables = buildTables(ctx, relevantSales, relevantPurchases);
        DashboardSummaryResponse.BudgetSummary budget = buildBudgetSummary(ctx, kpis.totalExpenses());
        DashboardSummaryResponse.Workforce workforce = buildWorkforce(ctx);
        DashboardSummaryResponse.YearScope yearScope = new DashboardSummaryResponse.YearScope(selectedYear, availableYears);

        DashboardSummaryResponse response = new DashboardSummaryResponse(kpis, charts, tables, budget, workforce, yearScope);
        return ResponseEntity.ok(new ApiResponse<>(response, "Dashboard summary fetched successfully", true, java.time.Instant.now()));
    }

    public ResponseEntity<ApiResponse<PagedTransactionsResponse>> getTransactions(
            User currentUser, int page, int size, String filter, String search) {

        DashboardScopeContext ctx = resolveScopeContext(currentUser);
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

    public ResponseEntity<ApiResponse<DashboardInventorySummaryResponse>> getInventorySummary(User currentUser) {
        DashboardScopeContext ctx = resolveScopeContext(currentUser);

        // 1. Supplies / Inputs Summary
        List<InventoryItem> items = inventoryRepo.findAll().stream()
                .filter(item -> isInventoryInScope(item, ctx))
                .collect(Collectors.toList());

        long totalItems = items.size();
        BigDecimal totalValuation = BigDecimal.ZERO;
        long inStockCount = 0;
        long lowStockCount = 0;
        long outOfStockCount = 0;

        Map<String, List<InventoryItem>> byCategory = new HashMap<>();

        for (InventoryItem item : items) {
            BigDecimal qty = item.getQuantityInStock() != null ? item.getQuantityInStock() : BigDecimal.ZERO;
            BigDecimal price = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
            BigDecimal minStock = item.getMinStockLevel() != null ? item.getMinStockLevel() : BigDecimal.ZERO;

            totalValuation = totalValuation.add(qty.multiply(price));

            if (qty.compareTo(BigDecimal.ZERO) <= 0) {
                outOfStockCount++;
            } else if (qty.compareTo(minStock) <= 0) {
                lowStockCount++;
                inStockCount++;
            } else {
                inStockCount++;
            }

            String cat = item.getCategory() != null && !item.getCategory().trim().isEmpty() ? item.getCategory().trim() : "General Supplies";
            byCategory.computeIfAbsent(cat, k -> new ArrayList<>()).add(item);
        }

        List<DashboardInventorySummaryResponse.CategoryValuation> categoryValuations = new ArrayList<>();
        for (Map.Entry<String, List<InventoryItem>> entry : byCategory.entrySet()) {
            BigDecimal catVal = entry.getValue().stream()
                    .map(i -> (i.getQuantityInStock() != null ? i.getQuantityInStock() : BigDecimal.ZERO)
                            .multiply(i.getUnitPrice() != null ? i.getUnitPrice() : BigDecimal.ZERO))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            categoryValuations.add(new DashboardInventorySummaryResponse.CategoryValuation(
                    entry.getKey(),
                    entry.getValue().size(),
                    catVal
            ));
        }

        categoryValuations.sort((a, b) -> b.valuation().compareTo(a.valuation()));

        DashboardInventorySummaryResponse.SuppliesSummary suppliesSummary =
                new DashboardInventorySummaryResponse.SuppliesSummary(
                        totalItems,
                        totalValuation,
                        inStockCount,
                        lowStockCount,
                        outOfStockCount,
                        categoryValuations
                );

        // 2. Harvest Produce Ready for Market
        List<HarvestInventory> produceList = harvestInventoryRepo.findAll(org.springframework.data.domain.Sort.by("itemName").ascending());
        if (!ctx.isAdmin()) {
            Set<String> projectNames = ctx.relevantProjects().stream()
                    .map(Project::getName)
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet());
            produceList = produceList.stream()
                    .filter(p -> p.getProjectName() != null && projectNames.contains(p.getProjectName().trim().toLowerCase()))
                    .collect(Collectors.toList());
        }

        float totalProduceQuantity = 0.0f;
        List<DashboardInventorySummaryResponse.ProduceStockItem> availableProduce = new ArrayList<>();

        for (HarvestInventory h : produceList) {
            float qty = h.getAvailableQuantity();
            if (qty > 0) {
                totalProduceQuantity += qty;
                String unit = h.getDisplayUnit() != null ? h.getDisplayUnit() : (h.getUnits() != null ? h.getUnits() : "units");
                availableProduce.add(new DashboardInventorySummaryResponse.ProduceStockItem(
                        h.getItemName(),
                        qty,
                        unit,
                        h.getProjectName() != null ? h.getProjectName() : "General"
                ));
            }
        }

        DashboardInventorySummaryResponse.ProduceSummary produceSummary =
                new DashboardInventorySummaryResponse.ProduceSummary(
                        availableProduce.size(),
                        totalProduceQuantity,
                        availableProduce
                );

        DashboardInventorySummaryResponse response = new DashboardInventorySummaryResponse(suppliesSummary, produceSummary);
        return ResponseEntity.ok(new ApiResponse<>(response, "Inventory summary fetched successfully", true, java.time.Instant.now()));
    }

    // =========================================================================
    // 3. Scope & Category Access Resolvers
    // =========================================================================

    private List<String> resolveAvailableYears(int currentYear) {
        Integer pYear = projectRepo.findEarliestProjectYear();
        Integer eYear = expenseRepo.findEarliestExpenseYear();
        Integer sYear = salesRepo.findEarliestSaleYear();
        Integer purYear = purchaseRepo.findEarliestPurchaseYear();

        int earliest = currentYear;
        for (Integer y : Arrays.asList(pYear, eYear, sYear, purYear)) {
            if (y != null && y > 1900 && y < earliest) {
                earliest = y;
            }
        }
        if (earliest < currentYear - 100) {
            earliest = currentYear - 100;
        }

        List<String> years = new ArrayList<>();
        for (int y = currentYear + 1; y >= earliest; y--) {
            years.add(String.valueOf(y));
        }
        years.add("ALL");
        return years;
    }

    private String resolveSelectedYear(String yearParam, int currentYear, List<String> availableYears) {
        if (yearParam != null && !yearParam.trim().isEmpty()) {
            String trimmed = yearParam.trim().toUpperCase();
            if ("ALL".equals(trimmed) || availableYears.contains(trimmed)) {
                return trimmed;
            }
        }
        return String.valueOf(currentYear);
    }

    private DashboardScopeContext resolveScopeContext(User currentUser) {
        return resolveScopeContext(currentUser, null, null);
    }

    private DashboardScopeContext resolveScopeContext(User currentUser, LocalDate startDate, LocalDate endDate) {
        boolean isAdmin = currentUser != null && "ADMIN".equalsIgnoreCase(currentUser.getRole());
        boolean isManager = currentUser != null && "MANAGER".equalsIgnoreCase(currentUser.getRole());
        String userId = currentUser != null ? currentUser.getId() : null;

        List<Project> relevantProjects;
        if (startDate == null || endDate == null) {
            if (isAdmin) {
                relevantProjects = projectRepo.findAll();
            } else if (isManager) {
                relevantProjects = projectRepo.findProjectsListForManager(userId);
            } else {
                relevantProjects = projectRepo.findBySupervisorId(userId);
            }
        } else {
            if (isAdmin) {
                relevantProjects = projectRepo.findProjectsActiveBetween(startDate, endDate);
            } else if (isManager) {
                relevantProjects = projectRepo.findProjectsForManagerActiveBetween(userId, startDate, endDate);
            } else {
                relevantProjects = projectRepo.findProjectsForSupervisorActiveBetween(userId, startDate, endDate);
            }
        }

        Set<String> projectIds = relevantProjects.stream()
                .map(Project::getId)
                .collect(Collectors.toSet());

        Set<String> assignedCategoryIds = new HashSet<>();
        Set<String> assignedCategoryNames = new HashSet<>();

        if (isManager && currentUser != null) {
            java.util.Optional.of(currentUser).ifPresent(u -> {
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
                currentUser,
                isAdmin,
                isManager,
                relevantProjects,
                projectIds,
                assignedCategoryIds,
                assignedCategoryNames
        );
    }

    private List<Sale> fetchRelevantSales(DashboardScopeContext ctx) {
        return fetchRelevantSales(ctx, null, null);
    }

    private List<Sale> fetchRelevantSales(DashboardScopeContext ctx, LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            if (ctx.isAdmin()) {
                return salesRepo.findAll();
            }
            if (ctx.projectIds().isEmpty()) {
                return Collections.emptyList();
            }
            return salesRepo.findByProjectIdIn(ctx.projectIds());
        } else {
            if (ctx.isAdmin()) {
                return salesRepo.findAllBetween(startDate, endDate);
            }
            if (ctx.projectIds().isEmpty()) {
                return Collections.emptyList();
            }
            return salesRepo.findByProjectIdInBetween(ctx.projectIds(), startDate, endDate);
        }
    }

    private List<SupplierPurchase> fetchRelevantPurchases(DashboardScopeContext ctx) {
        return fetchRelevantPurchases(ctx, null, null);
    }

    private List<SupplierPurchase> fetchRelevantPurchases(DashboardScopeContext ctx, LocalDate startDate, LocalDate endDate) {
        List<SupplierPurchase> allPurchases = (startDate == null || endDate == null)
                ? purchaseRepo.findAllByOrderByPurchaseDateDesc()
                : purchaseRepo.findAllBetween(startDate, endDate);

        if (ctx.isAdmin()) {
            return allPurchases;
        }
        if (ctx.isManager()) {
            return allPurchases.stream()
                    .filter(p -> isPurchaseInManagerScope(p, ctx))
                    .collect(Collectors.toList());
        }
        return allPurchases.stream()
                .filter(p -> p.getRecordedBy() != null && ctx.currentUser() != null && ctx.currentUser().getId() != null && ctx.currentUser().getId().equals(p.getRecordedBy().getId()))
                .collect(Collectors.toList());
    }

    private boolean isPurchaseInManagerScope(SupplierPurchase p, DashboardScopeContext ctx) {
        if (p.getRecordedBy() != null && ctx.currentUser() != null && ctx.currentUser().getId() != null && ctx.currentUser().getId().equals(p.getRecordedBy().getId())) {
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
            List<SupplierPurchase> purchases,
            LocalDate startDate,
            LocalDate endDate) {

        BigDecimal totalRevenue = calculateTotalRevenue(sales);
        BigDecimal pendingDebt = calculateCustomerDebt(ctx, sales);
        BigDecimal receivedRevenue = calculateReceivedRevenue(sales);
        BigDecimal supplierDebt = calculateSupplierDebt(ctx, purchases);
        long supplierDebtCount = countSupplierDebtAccounts(ctx, purchases);
        long lowStockCount = countLowStockItems(ctx);

        BigDecimal totalExpenses = calculateExpenses(ctx, startDate, endDate);
        BigDecimal totalSuppliesCost = calculateSuppliesCost(ctx, purchases, startDate, endDate);
        BigDecimal totalOutflows = totalExpenses.add(totalSuppliesCost);

        BigDecimal netProfit = receivedRevenue.subtract(totalOutflows);
        Double operatingMargin = 0.0;
        if (totalRevenue.compareTo(BigDecimal.ZERO) > 0) {
            operatingMargin = Math.round((netProfit.doubleValue() / totalRevenue.doubleValue() * 100.0) * 10.0) / 10.0;
        }
        BigDecimal netWorkingCapital = receivedRevenue.add(pendingDebt).subtract(supplierDebt);

        return new DashboardSummaryResponse.Kpis(
                totalRevenue,
                receivedRevenue,
                pendingDebt,
                supplierDebt,
                lowStockCount,
                supplierDebtCount,
                netProfit,
                operatingMargin,
                netWorkingCapital,
                totalExpenses,
                totalSuppliesCost,
                totalOutflows
        );
    }

    private BigDecimal calculateExpenses(DashboardScopeContext ctx, LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            if (ctx.isAdmin()) {
                return expenseRepo.totalAllExpenses();
            }
            if (ctx.projectIds().isEmpty()) {
                return BigDecimal.ZERO;
            }
            return expenseRepo.totalExpensesByProjectIds(ctx.projectIds());
        } else {
            if (ctx.isAdmin()) {
                return expenseRepo.totalExpensesBetween(startDate, endDate);
            }
            if (ctx.projectIds().isEmpty()) {
                return BigDecimal.ZERO;
            }
            return expenseRepo.totalExpensesByProjectIdsBetween(ctx.projectIds(), startDate, endDate);
        }
    }

    private BigDecimal calculateSuppliesCost(DashboardScopeContext ctx, List<SupplierPurchase> purchases, LocalDate startDate, LocalDate endDate) {
        if (ctx.isAdmin()) {
            if (startDate == null || endDate == null) {
                return purchaseRepo.totalAllPurchasePaid();
            }
            return purchaseRepo.totalPurchasePaidBetween(startDate, endDate);
        }
        return purchases.stream()
                .map(p -> p.getAmountPaid() != null ? p.getAmountPaid() : (p.getInvoiceAmount() != null ? p.getInvoiceAmount() : BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
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
            return customerRepo.totalOutstandingDebt();
        }
        return sales.stream()
                .map(Sale::getBalanceDue)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateSupplierDebt(DashboardScopeContext ctx, List<SupplierPurchase> purchases) {
        if (ctx.isAdmin()) {
            return supplierRepo.totalBalanceOwed();
        }
        return purchases.stream()
                .filter(p -> p.getSupplier() == null || p.getSupplier().isActive())
                .map(SupplierPurchase::getBalanceDue)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private long countSupplierDebtAccounts(DashboardScopeContext ctx, List<SupplierPurchase> purchases) {
        if (ctx.isAdmin()) {
            return supplierRepo.countSuppliersWithDebt();
        }
        return purchases.stream()
                .filter(p -> p.getSupplier() == null || p.getSupplier().isActive())
                .filter(p -> p.getBalanceDue() != null && p.getBalanceDue().compareTo(BigDecimal.ZERO) > 0)
                .map(p -> p.getSupplier() != null ? p.getSupplier().getId() : p.getId())
                .distinct()
                .count();
    }

    private long countLowStockItems(DashboardScopeContext ctx) {
        if (ctx.isAdmin()) {
            return inventoryRepo.countLowStockItems();
        }
        return inventoryRepo.findAll().stream()
                .filter(item -> isInventoryInScope(item, ctx))
                .filter(item -> item.getQuantityInStock() != null && item.getMinStockLevel() != null)
                .filter(item -> item.getQuantityInStock().compareTo(item.getMinStockLevel()) <= 0)
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
        List<DashboardSummaryResponse.RecentHarvest> recentHarvests = buildRecentHarvests(ctx, 5);
        List<DashboardSummaryResponse.RecentSaleTransaction> recentSales = buildRecentSales(sales, 5);
        List<DashboardSummaryResponse.RecentSupplyTransaction> recentSupplies = buildRecentSupplies(purchases, 5);
        List<DashboardSummaryResponse.RecentTransaction> recentTransactions = buildCombinedTransactions(recentSales, recentSupplies);
        List<DashboardSummaryResponse.OperationalActivity> operationalActivities = buildOperationalActivities(ctx, 8);

        return new DashboardSummaryResponse.Tables(
                statusSplit,
                recentHarvests,
                recentSales,
                recentSupplies,
                recentTransactions,
                operationalActivities
        );
    }

    private List<DashboardSummaryResponse.OperationalActivity> buildOperationalActivities(DashboardScopeContext ctx, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<Activity> activities;
        if (ctx.isAdmin()) {
            activities = activityRepo.findOperationalActivities(pageable);
        } else if (ctx.projectIds().isEmpty()) {
            activities = Collections.emptyList();
        } else {
            activities = activityRepo.findOperationalActivitiesByProjectIds(ctx.projectIds(), pageable);
        }

        List<DashboardSummaryResponse.OperationalActivity> result = new ArrayList<>();
        for (Activity a : activities) {
            long workers = laborRepo.countByActivityId(a.getId());
            String projName = a.getProject() != null ? a.getProject().getName() : "General";
            String farmLoc = (a.getProject() != null && a.getProject().getCategory() != null)
                    ? a.getProject().getCategory().getName()
                    : "Main Farm";

            result.add(new DashboardSummaryResponse.OperationalActivity(
                    a.getId(),
                    a.getTitle(),
                    a.getType() != null ? a.getType() : "GENERAL",
                    a.getStatus() != null ? a.getStatus() : "SCHEDULED",
                    a.getPriority() != null ? a.getPriority() : "MEDIUM",
                    a.getScheduledDate() != null ? a.getScheduledDate().toString() : "",
                    a.getDueDate() != null ? a.getDueDate().toString() : "",
                    a.getNotes() != null ? a.getNotes() : "",
                    projName,
                    farmLoc,
                    workers
            ));
        }
        return result;
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

    private List<DashboardSummaryResponse.RecentHarvest> buildRecentHarvests(DashboardScopeContext ctx, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<Harvest> list;
        if (ctx.isAdmin()) {
            list = harvestRepo.findRecent(pageable);
        } else if (ctx.projectIds().isEmpty()) {
            list = Collections.emptyList();
        } else {
            list = harvestRepo.findRecentByProjectIds(ctx.projectIds(), pageable);
        }

        return list.stream()
                .map(h -> new DashboardSummaryResponse.RecentHarvest(
                        h.getId(),
                        h.getProject() != null ? h.getProject().getName() : "Unknown",
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

    private DashboardSummaryResponse.BudgetSummary buildBudgetSummary(DashboardScopeContext ctx, BigDecimal totalSpent) {
        BigDecimal totalBudget = ctx.relevantProjects().stream()
                .map(Project::getBudget)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new DashboardSummaryResponse.BudgetSummary(totalBudget, totalSpent != null ? totalSpent : BigDecimal.ZERO);
    }

    private DashboardSummaryResponse.Workforce buildWorkforce(DashboardScopeContext ctx) {
        long totalManagers = userRepo.countByRoleIgnoreCase("MANAGER");
        long totalSupervisors = userRepo.countByRoleIgnoreCase("SUPERVISOR");
        long mySupervisors = ctx.isManager() && ctx.currentUser() != null ? userRepo.countByCreatedById(ctx.currentUser().getId()) : 0;
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
