package com.recruitment.interview.messaging;

import java.util.UUID;

public record NotificationEvent(
        UUID tenantId,
        UUID recipientUserId,
        String actionType,
        String entityType,
        UUID entityId,
        String title,
        String message) {
}
