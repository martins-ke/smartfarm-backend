package com.smartfarm.notifications;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.smartfarm.ApiResponse;
import com.smartfarm.activities.Activity;
import com.smartfarm.activities.ActivityRepository;
import com.smartfarm.customers.Customer;
import com.smartfarm.customers.CustomerRepository;
import com.smartfarm.inventory.InventoryItem;
import com.smartfarm.inventory.InventoryItemRepository;
import com.smartfarm.suppliers.SupplierPurchase;
import com.smartfarm.suppliers.SupplierPurchaseRepository;
import com.smartfarm.user.User;
import com.smartfarm.user.UserRepository;

@Service
public class NotificationService {

	private final UserRepository userRepo;
	private final InventoryItemRepository inventoryRepo;
	private final ActivityRepository activityRepo;
	private final SupplierPurchaseRepository purchaseRepo;
	private final CustomerRepository customerRepo;

	public NotificationService(UserRepository userRepo, InventoryItemRepository inventoryRepo,
			ActivityRepository activityRepo, SupplierPurchaseRepository purchaseRepo,
			CustomerRepository customerRepo) {
		this.userRepo = userRepo;
		this.inventoryRepo = inventoryRepo;
		this.activityRepo = activityRepo;
		this.purchaseRepo = purchaseRepo;
		this.customerRepo = customerRepo;
	}

	public ResponseEntity<ApiResponse<NotificationSummaryResponse>> getNotifications(String userId, String userRole) {
		boolean isAdmin = "ADMIN".equalsIgnoreCase(userRole);
		boolean isManager = "MANAGER".equalsIgnoreCase(userRole);
		List<NotificationResponse> list = new ArrayList<>();
		LocalDate today = LocalDate.now();

		// 1. Account Creation Requests (Admin & Manager)
		if (isAdmin || isManager) {
			List<User> pendingUsers = userRepo.findByStatusIgnoreCase("PENDING_APPROVAL");
			if (!pendingUsers.isEmpty()) {
				list.add(new NotificationResponse(
					"NOTIF-USERS-PENDING",
					"USERS",
					"WARNING",
					"Account Requests Awaiting Approval",
					pendingUsers.size() + " user registration(s) pending review and approval.",
					Instant.now().toString(),
					"/users",
					"Review Users",
					null
				));
			}
		}

		// 2. Low Stock Alerts (All Roles)
		List<InventoryItem> allItems = inventoryRepo.findAll();
		List<InventoryItem> lowStockItems = new ArrayList<>();
		for (InventoryItem item : allItems) {
			if (item.getQuantityInStock() != null && item.getMinStockLevel() != null) {
				if (item.getQuantityInStock().compareTo(item.getMinStockLevel()) <= 0) {
					lowStockItems.add(item);
				}
			}
		}

		if (!lowStockItems.isEmpty()) {
			String firstItems = lowStockItems.stream()
					.limit(3)
					.map(InventoryItem::getName)
					.reduce((a, b) -> a + ", " + b)
					.orElse("Items");
			String suffix = lowStockItems.size() > 3 ? (" and " + (lowStockItems.size() - 3) + " more") : "";

			list.add(new NotificationResponse(
				"NOTIF-STOCK-LOW",
				"INVENTORY",
				"DANGER",
				"Low Stock Alert (" + lowStockItems.size() + " Items)",
				firstItems + suffix + " are below minimum reorder threshold.",
				Instant.now().toString(),
				"/inventory",
				"Restock Inventory",
				null
			));
		}

		// 3. Farm Task Schedules & Reminders
		// Fetch overdue tasks
		List<Activity> overdueTasks = activityRepo.findOverdueTasks(today);
		for (Activity task : overdueTasks) {
			long daysOverdue = ChronoUnit.DAYS.between(task.getScheduledDate(), today);
			String projectName = task.getProject() != null ? task.getProject().getName() : "Farm Field";
			String catSlug = (task.getProject() != null && task.getProject().getCategory() != null)
					? task.getProject().getCategory().getName().toLowerCase()
					: "general";
			String projId = task.getProject() != null ? task.getProject().getId() : "";

			list.add(new NotificationResponse(
				"NOTIF-TASK-OVERDUE-" + task.getId(),
				"TASKS",
				"DANGER",
				"⚠️ Overdue: " + task.getTitle(),
				task.getType() + " on " + projectName + " was due " + daysOverdue + " day(s) ago (" + task.getScheduledDate() + ").",
				Instant.now().toString(),
				projId.isEmpty() ? "/categories" : ("/categories/" + catSlug + "/projects/" + projId),
				"View Task",
				task.getId()
			));
		}

		// Fetch tasks due in the next 7 days (including today)
		List<Activity> upcomingTasks = activityRepo.findUpcomingScheduledTasks(today, today.plusDays(7));
		for (Activity task : upcomingTasks) {
			long daysUntil = ChronoUnit.DAYS.between(today, task.getScheduledDate());
			String timingText = daysUntil == 0 ? "Due Today" : ("Due in " + daysUntil + " day(s)");
			String severity = daysUntil == 0 ? "WARNING" : "INFO";
			String projectName = task.getProject() != null ? task.getProject().getName() : "Farm Field";
			String catSlug = (task.getProject() != null && task.getProject().getCategory() != null)
					? task.getProject().getCategory().getName().toLowerCase()
					: "general";
			String projId = task.getProject() != null ? task.getProject().getId() : "";

			list.add(new NotificationResponse(
				"NOTIF-TASK-DUE-" + task.getId(),
				"TASKS",
				severity,
				(daysUntil == 0 ? "📅 " : "🕒 ") + timingText + ": " + task.getTitle(),
				task.getType() + " scheduled for " + projectName + " on " + task.getScheduledDate() + ".",
				Instant.now().toString(),
				projId.isEmpty() ? "/categories" : ("/categories/" + catSlug + "/projects/" + projId),
				"Open Project",
				task.getId()
			));
		}

		// 4. Financial Alerts (Admin & Manager)
		if (isAdmin || isManager) {
			List<SupplierPurchase> unpaidPurchases = purchaseRepo.findByPaymentStatusIgnoreCase("UNPAID");
			BigDecimal unpaidTotal = unpaidPurchases.stream()
					.map(SupplierPurchase::getBalanceDue)
					.reduce(BigDecimal.ZERO, BigDecimal::add);

			if (unpaidTotal.compareTo(BigDecimal.ZERO) > 0) {
				list.add(new NotificationResponse(
					"NOTIF-FINANCE-AP",
					"FINANCE",
					"WARNING",
					"Pending Supplier Invoices",
					unpaidPurchases.size() + " unpaid purchase invoice(s) totaling KES " + unpaidTotal.toPlainString() + " awaiting payment.",
					Instant.now().toString(),
					"/suppliers",
					"Supplier Ledger",
					null
				));
			}

			List<Customer> debtCustomers = customerRepo.findAll().stream()
					.filter(c -> c.getOutstandingDebt() != null && c.getOutstandingDebt().compareTo(BigDecimal.ZERO) > 0)
					.toList();

			if (!debtCustomers.isEmpty()) {
				BigDecimal totalDebt = debtCustomers.stream()
						.map(Customer::getOutstandingDebt)
						.reduce(BigDecimal.ZERO, BigDecimal::add);

				list.add(new NotificationResponse(
					"NOTIF-FINANCE-AR",
					"FINANCE",
					"INFO",
					"Customer Credit Ledger",
					debtCustomers.size() + " customer(s) have uncollected credit balances totaling KES " + totalDebt.toPlainString() + ".",
					Instant.now().toString(),
					"/customers",
					"Customer Ledger",
					null
				));
			}
		}

		NotificationSummaryResponse summary = new NotificationSummaryResponse(
			list.size(),
			list.size(),
			list
		);

		return ResponseEntity.ok(new ApiResponse<>(summary, "Notifications retrieved successfully ✅", true, Instant.now()));
	}
}
