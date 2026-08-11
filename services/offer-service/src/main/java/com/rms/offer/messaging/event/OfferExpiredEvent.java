package com.rms.offer.messaging.event;

import java.util.UUID;

public record OfferExpiredEvent(
        UUID offerId,
        UUID applicationId,
        UUID tenantId
) {}
