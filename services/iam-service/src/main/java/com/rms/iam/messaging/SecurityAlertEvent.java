package com.rms.iam.messaging;

import java.util.UUID;

public record SecurityAlertEvent(
        UUID userId,
        String alertType,
        String description
) {}
