package com.smartfarm.dashboard;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smartfarm.ApiResponse;
import com.smartfarm.user.User;

@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<DashboardSummaryResponse>> getDashboardSummary(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(value = "year", required = false) String year) {
        return dashboardService.getSummary(currentUser, year);
    }

    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<PagedTransactionsResponse>> getTransactions(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "5") int size,
            @RequestParam(value = "filter", defaultValue = "ALL") String filter,
            @RequestParam(value = "search", required = false) String search) {
        return dashboardService.getTransactions(currentUser, page, size, filter, search);
    }

    @GetMapping("/inventory-summary")
    public ResponseEntity<ApiResponse<DashboardInventorySummaryResponse>> getInventorySummary(
            @AuthenticationPrincipal User currentUser) {
        return dashboardService.getInventorySummary(currentUser);
    }
}
