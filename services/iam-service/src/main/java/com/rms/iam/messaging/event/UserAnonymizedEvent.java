package com.rms.iam.messaging.event;

import java.time.Instant;
import java.util.UUID;

public record UserAnonymizedEvent(
        String eventType,
        UUID userId,
        String email,
        Instant timestamp
) {}
