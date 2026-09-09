package com.smartfarm.notifications;

public record NotificationResponse(
    String id,
    String category, // "USERS", "INVENTORY", "TASKS", "FINANCE"
    String severity, // "INFO", "WARNING", "DANGER", "SUCCESS"
    String title,
    String message,
    String timestamp,
    String actionUrl,
    String actionLabel,
    String entityId
) {}
