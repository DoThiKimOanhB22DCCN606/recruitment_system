package com.rms.iam.messaging;

import java.time.Instant;
import java.util.UUID;

public record AccountLockedEvent(
        UUID userId,
        String email,
        Instant lockoutUntil
) {}
