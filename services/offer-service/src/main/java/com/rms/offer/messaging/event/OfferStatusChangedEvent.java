package com.rms.offer.messaging.event;

import java.util.UUID;

public record OfferStatusChangedEvent(UUID offerId, UUID applicationId, UUID tenantId, String oldStatus, String newStatus) {}
