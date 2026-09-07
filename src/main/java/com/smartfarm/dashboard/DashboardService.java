package com.smartfarm.dashboard;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.smartfarm.ApiResponse;
import com.smartfarm.customers.CustomerRepository;
import com.smartfarm.inventory.InventoryItem;
import com.smartfarm.inventory.InventoryItemRepository;
import com.smartfarm.projects.Project;
import com.smartfarm.projects.ProjectRepository;
import com.smartfarm.sales.Sale;
import com.smartfarm.sales.SalesRepository;

@Service
public class DashboardService {

    private final ProjectRepository projectRepo;
    private final SalesRepository salesRepo;
    private final InventoryItemRepository inventoryRepo;
    private final CustomerRepository customerRepo;

    public DashboardService(ProjectRepository projectRepo, SalesRepository salesRepo,
                            InventoryItemRepository inventoryRepo, CustomerRepository customerRepo) {
        this.projectRepo = projectRepo;
        this.salesRepo = salesRepo;
        this.inventoryRepo = inventoryRepo;
        this.customerRepo = customerRepo;
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

        // 5. Inventory Low Stock
        List<InventoryItem> allItems = inventoryRepo.findAll();
        List<DashboardSummaryResponse.LowStockItem> lowStockItems = new ArrayList<>();
        
        for (InventoryItem item : allItems) {
            if (item.getQuantityInStock() != null && item.getMinStockLevel() != null) {
                if (item.getQuantityInStock().compareTo(item.getMinStockLevel()) <= 0) {
                    lowStockItems.add(new DashboardSummaryResponse.LowStockItem(
                        item.getId(),
                        item.getName(),
                        item.getCategory(),
                        item.getQuantityInStock().floatValue(),
                        item.getMinStockLevel().floatValue()
                    ));
                }
            }
        }
        
        long lowStockCount = lowStockItems.size();

        // Limit low stock table to top 5 items for the dashboard
        List<DashboardSummaryResponse.LowStockItem> lowStockTable = lowStockItems.stream()
                .limit(5)
                .collect(Collectors.toList());

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

        // Assembly
        DashboardSummaryResponse.Kpis kpis = new DashboardSummaryResponse.Kpis(
                totalRevenue, activeProjects, lowStockCount, customerCount
        );
        
        DashboardSummaryResponse.Charts charts = new DashboardSummaryResponse.Charts(
                salesTrend, revenueByCategory
        );
        
        DashboardSummaryResponse.Tables tables = new DashboardSummaryResponse.Tables(
                lowStockTable, statusSplit
        );

        DashboardSummaryResponse response = new DashboardSummaryResponse(kpis, charts, tables);

        return ResponseEntity.ok(new ApiResponse<>(response, "Dashboard summary fetched successfully", true, java.time.Instant.now()));
    }
}
