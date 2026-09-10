package com.smartfarm.dashboard;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.bind.annotation.RequestParam;

import com.smartfarm.ApiResponse;

@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<DashboardSummaryResponse>> getDashboardSummary(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        
        // Default to Supervisor if omitted to be safe, or handle error
        if (userId == null || userRole == null) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(null, "Missing user identity headers", false, java.time.Instant.now()));
        }

        return dashboardService.getSummary(userId, userRole);
    }

    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<PagedTransactionsResponse>> getTransactions(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "5") int size,
            @RequestParam(value = "filter", defaultValue = "ALL") String filter,
            @RequestParam(value = "search", required = false) String search) {
        
        if (userId == null || userRole == null) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(null, "Missing user identity headers", false, java.time.Instant.now()));
        }

        return dashboardService.getTransactions(userId, userRole, page, size, filter, search);
    }
}
