package com.smartfarm.notifications;

import java.util.List;

public record NotificationSummaryResponse(
    long unreadCount,
    long totalCount,
    List<NotificationResponse> notifications
) {}
