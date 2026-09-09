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
import com.smartfarm.harvest.Harvest;
import com.smartfarm.harvest.HarvestRepository;
import com.smartfarm.suppliers.SupplierPurchase;
import com.smartfarm.suppliers.SupplierPurchaseRepository;
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

    public DashboardService(ProjectRepository projectRepo, SalesRepository salesRepo,
                            InventoryItemRepository inventoryRepo, CustomerRepository customerRepo,
                            ExpenseRepository expenseRepo, HarvestRepository harvestRepo,
                            UserRepository userRepo, SupplierPurchaseRepository purchaseRepo) {
        this.projectRepo = projectRepo;
        this.salesRepo = salesRepo;
        this.inventoryRepo = inventoryRepo;
        this.customerRepo = customerRepo;
        this.expenseRepo = expenseRepo;
        this.harvestRepo = harvestRepo;
        this.userRepo = userRepo;
        this.purchaseRepo = purchaseRepo;
    }

    public ResponseEntity<ApiResponse<DashboardSummaryResponse>> getSummary(String userId, String userRole) {
        boolean isAdmin = "ADMIN".equalsIgnoreCase(userRole);
        boolean isManager = "MANAGER".equalsIgnoreCase(userRole);
        boolean isSupervisor = "SUPERVISOR".equalsIgnoreCase(userRole);

        // 1. Fetch relevant projects based on role
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

        // 2. Fetch Sales (filter by relevant projects if not admin)
        List<Sale> allSales = salesRepo.findAll();
        List<Sale> relevantSales = isAdmin ? allSales : allSales.stream()
                .filter(s -> s.getProject() != null && projectIds.contains(s.getProject().getId()))
                .collect(Collectors.toList());

        BigDecimal totalRevenue = relevantSales.stream()
                .map(Sale::getTotal_amount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Ground-truth total debt owed by customers across the system
        BigDecimal pendingDebt;
        if (isAdmin) {
            List<Customer> allCustomers = customerRepo.findAll();
            BigDecimal customerDebt = allCustomers.stream()
                    .map(Customer::getOutstandingDebt)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            BigDecimal salesDebt = relevantSales.stream()
                    .map(s -> s.getBalanceDue() != null ? s.getBalanceDue() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            pendingDebt = customerDebt.compareTo(BigDecimal.ZERO) > 0 ? customerDebt : salesDebt;
        } else {
            pendingDebt = relevantSales.stream()
                    .map(s -> s.getBalanceDue() != null ? s.getBalanceDue() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        // Actual cash collected into the account (Total Sales - Outstanding Customer Debt)
        BigDecimal receivedRevenue = totalRevenue.subtract(pendingDebt);
        if (receivedRevenue.compareTo(BigDecimal.ZERO) < 0) {
            receivedRevenue = relevantSales.stream()
                    .map(s -> s.getAmountPaid() != null ? s.getAmountPaid() : (s.getTotal_amount() != null ? s.getTotal_amount() : BigDecimal.ZERO))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        // 3. Project Status Split
        long activeProjects = 0;
        long pendingProjects = 0;
        long completedProjects = 0;

        for (Project p : relevantProjects) {
            if ("ACTIVE".equalsIgnoreCase(p.getStatus())) activeProjects++;
            else if ("COMPLETED".equalsIgnoreCase(p.getStatus())) completedProjects++;
            else pendingProjects++; // PENDING_APPROVAL or others
        }

        DashboardSummaryResponse.ProjectStatusSplit statusSplit = new DashboardSummaryResponse.ProjectStatusSplit(
                pendingProjects, activeProjects, completedProjects
        );

        // 4. Customers Count (Global)
        long customerCount = customerRepo.count();

        // 5. Inventory Low Stock Count
        List<InventoryItem> allItems = inventoryRepo.findAll();
        long lowStockCount = 0;
        
        for (InventoryItem item : allItems) {
            if (item.getQuantityInStock() != null && item.getMinStockLevel() != null) {
                if (item.getQuantityInStock().compareTo(item.getMinStockLevel()) <= 0) {
                    lowStockCount++;
                }
            }
        }

        // 6. Charts - Revenue by Category
        Map<String, BigDecimal> categoryRevenueMap = new HashMap<>();
        for (Sale s : relevantSales) {
            if (s.getProject() != null && s.getProject().getCategory() != null) {
                String catName = s.getProject().getCategory().getName();
                BigDecimal amount = s.getTotal_amount() != null ? s.getTotal_amount() : BigDecimal.ZERO;
                categoryRevenueMap.put(catName, categoryRevenueMap.getOrDefault(catName, BigDecimal.ZERO).add(amount));
            }
        }
        
        List<DashboardSummaryResponse.CategoryRevenueData> revenueByCategory = categoryRevenueMap.entrySet().stream()
                .map(e -> new DashboardSummaryResponse.CategoryRevenueData(e.getKey(), e.getValue()))
                .collect(Collectors.toList());

        // 7. Charts - Sales Trend (last 10 days grouped by date)
        Map<String, BigDecimal> salesTrendMap = new TreeMap<>();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        
        for (Sale s : relevantSales) {
            if (s.getAddedOn() != null) {
                String dateStr = s.getAddedOn().format(dtf);
                BigDecimal amount = s.getTotal_amount() != null ? s.getTotal_amount() : BigDecimal.ZERO;
                salesTrendMap.put(dateStr, salesTrendMap.getOrDefault(dateStr, BigDecimal.ZERO).add(amount));
            }
        }
        
        // Take the last 10 dates
        List<DashboardSummaryResponse.SalesTrendData> salesTrend = salesTrendMap.entrySet().stream()
                .skip(Math.max(0, salesTrendMap.size() - 10))
                .map(e -> new DashboardSummaryResponse.SalesTrendData(e.getKey(), e.getValue()))
                .collect(Collectors.toList());

        // 8. Budget & Expenditure
        BigDecimal totalBudget = relevantProjects.stream()
                .map(Project::getBudget)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalSpent = projectIds.stream()
                .map(expenseRepo::findByProjectId)
                .flatMap(List::stream)
                .map(Expense::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        DashboardSummaryResponse.BudgetSummary budgetSummary = new DashboardSummaryResponse.BudgetSummary(totalBudget, totalSpent);

        // 9. Workforce & Capacity
        long totalManagers = userRepo.countByRoleIgnoreCase("MANAGER");
        long totalSupervisors = userRepo.countByRoleIgnoreCase("SUPERVISOR");
        long mySupervisors = isManager ? userRepo.countByCreatedById(userId) : 0;
        DashboardSummaryResponse.Workforce workforce = new DashboardSummaryResponse.Workforce(totalManagers, totalSupervisors, mySupervisors);

        // 10. Recent Harvests (Last 5)
        List<Harvest> allHarvests = harvestRepo.findAll();
        List<DashboardSummaryResponse.RecentHarvest> recentHarvests = allHarvests.stream()
                .filter(h -> h.getProject() != null && projectIds.contains(h.getProject().getId()))
                .sorted((h1, h2) -> {
                    if (h1.getAdded_on() == null) return 1;
                    if (h2.getAdded_on() == null) return -1;
                    return h2.getAdded_on().compareTo(h1.getAdded_on());
                })
                .limit(5)
                .map(h -> new DashboardSummaryResponse.RecentHarvest(
                        h.getId(),
                        h.getProject().getName(),
                        h.getItem(),
                        h.getQuantity(),
                        h.getUnits(),
                        h.getAdded_on() != null ? h.getAdded_on().toString() : "N/A"
                ))
                .collect(Collectors.toList());

        // 11. Transaction History (Sales & Supplies)
        List<DashboardSummaryResponse.RecentSaleTransaction> recentSales = relevantSales.stream()
                .sorted((s1, s2) -> {
                    if (s1.getAddedOn() == null) return 1;
                    if (s2.getAddedOn() == null) return -1;
                    return s2.getAddedOn().compareTo(s1.getAddedOn());
                })
                .limit(10)
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

        List<SupplierPurchase> allPurchases = purchaseRepo.findAllByOrderByPurchaseDateDesc();
        List<DashboardSummaryResponse.RecentSupplyTransaction> recentSupplies = allPurchases.stream()
                .limit(10)
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

        List<DashboardSummaryResponse.RecentTransaction> recentTransactions = new ArrayList<>();

        for (DashboardSummaryResponse.RecentSaleTransaction s : recentSales) {
            String status = "PAID";
            if ("CREDIT_UNPAID".equalsIgnoreCase(s.paymentStatus())) {
                status = "UNPAID";
            } else if ("PARTIAL_PAYMENT".equalsIgnoreCase(s.paymentStatus()) || (s.balanceDue() != null && s.balanceDue().compareTo(BigDecimal.ZERO) > 0)) {
                status = "PARTIAL";
            }

            recentTransactions.add(new DashboardSummaryResponse.RecentTransaction(
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

        for (DashboardSummaryResponse.RecentSupplyTransaction p : recentSupplies) {
            recentTransactions.add(new DashboardSummaryResponse.RecentTransaction(
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

        recentTransactions.sort((t1, t2) -> {
            if (t1.date() == null || t1.date().isEmpty()) return 1;
            if (t2.date() == null || t2.date().isEmpty()) return -1;
            return t2.date().compareTo(t1.date());
        });

        // Assembly
        DashboardSummaryResponse.Kpis kpis = new DashboardSummaryResponse.Kpis(
                totalRevenue, receivedRevenue, pendingDebt, activeProjects, lowStockCount, customerCount
        );
        
        DashboardSummaryResponse.Charts charts = new DashboardSummaryResponse.Charts(
                salesTrend, revenueByCategory
        );
        
        DashboardSummaryResponse.Tables tables = new DashboardSummaryResponse.Tables(
                statusSplit, recentHarvests, recentSales, recentSupplies, recentTransactions
        );

        DashboardSummaryResponse response = new DashboardSummaryResponse(kpis, charts, tables, budgetSummary, workforce);

        return ResponseEntity.ok(new ApiResponse<>(response, "Dashboard summary fetched successfully", true, java.time.Instant.now()));
    }
}
