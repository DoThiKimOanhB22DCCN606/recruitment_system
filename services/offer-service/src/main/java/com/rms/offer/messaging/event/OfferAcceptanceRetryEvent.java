package com.rms.offer.messaging.event;

import java.util.UUID;

public record OfferAcceptanceRetryEvent(
        UUID offerId,
        UUID applicationId,
        UUID tenantId
) {}
