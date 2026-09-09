package com.smartfarm.notifications;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smartfarm.ApiResponse;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

	private final NotificationService notificationService;

	public NotificationController(NotificationService notificationService) {
		this.notificationService = notificationService;
	}

	@GetMapping
	public ResponseEntity<ApiResponse<NotificationSummaryResponse>> getNotifications(
			@RequestHeader(value = "X-User-Id", required = false) String headerUserId,
			@RequestHeader(value = "X-User-Role", required = false) String headerUserRole,
			@RequestParam(required = false) String userId,
			@RequestParam(required = false) String userRole) {
		String effectiveUserId = userId != null ? userId : headerUserId;
		String effectiveUserRole = userRole != null ? userRole : headerUserRole;
		return notificationService.getNotifications(effectiveUserId, effectiveUserRole);
	}
}
