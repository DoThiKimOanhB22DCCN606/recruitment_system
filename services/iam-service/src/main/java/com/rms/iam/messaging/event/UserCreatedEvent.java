package com.rms.iam.messaging.event;

import java.time.Instant;
import java.util.UUID;

public record UserCreatedEvent(
    String eventType,
    UUID userId, 
    String email,
    String role, 
    UUID tenantId,
    String correlationId,
    Instant occurredAt
) {}
