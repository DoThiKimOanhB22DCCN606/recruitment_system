package com.recruitment.ats.messaging;

import com.recruitment.ats.service.ApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OfferEventListener {

    private final ApplicationService applicationService;

    public record OfferExpiredEvent(
            UUID offerId,
            UUID applicationId,
            UUID tenantId
    ) {}

    @RabbitListener(queues = RabbitMQConfig.ATS_OFFER_EXPIRED_QUEUE)
    public void onOfferExpired(OfferExpiredEvent event) {
        if (event == null || event.applicationId() == null) {
            return;
        }
        log.info("Received OfferExpiredEvent for offerId={}, applicationId={}, tenantId={}",
                event.offerId(), event.applicationId(), event.tenantId());
        applicationService.handleOfferExpired(event.offerId(), event.applicationId(), event.tenantId());
    }
}
