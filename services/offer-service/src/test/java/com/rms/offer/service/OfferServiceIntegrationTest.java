package com.rms.offer.service;

import com.rms.offer.client.AtsServiceClient;
import com.rms.offer.dto.OfferRequest;
import com.rms.offer.entity.Offer;
import com.rms.offer.entity.OfferStatus;
import com.rms.offer.repository.OfferRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@Testcontainers
public class OfferServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Container
    static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3-management-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.rabbitmq.host", rabbitmq::getHost);
        registry.add("spring.rabbitmq.port", rabbitmq::getAmqpPort);
    }

    @Autowired
    private OfferService offerService;

    @Autowired
    private OfferRepository offerRepository;

    @MockBean
    private AtsServiceClient atsServiceClient;

    @AfterEach
    void cleanUp() {
        offerRepository.deleteAll();
    }

    @Test
    void testCreateAndSubmitOffer() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();

        when(atsServiceClient.validateApplication(any(UUID.class), any(UUID.class))).thenReturn(true);

        OfferRequest req = new OfferRequest();
        req.setApplicationId(applicationId);
        req.setCandidateId(UUID.randomUUID());
        req.setProposedSalary(new BigDecimal("100000.00"));
        req.setCurrency("USD");
        req.setStartDate(LocalDate.now().plusDays(30));
        req.setExpiryDate(LocalDate.now().plusDays(7));

        Offer created = offerService.createDraftOffer(req, tenantId, userId);

        assertThat(created.getId()).isNotNull();
        assertThat(created.getStatus()).isEqualTo(OfferStatus.DRAFT);

        offerService.sendOffer(created.getId(), tenantId);

        Optional<Offer> submitted = offerRepository.findById(created.getId());
        assertThat(submitted).isPresent();
        assertThat(submitted.get().getStatus()).isEqualTo(OfferStatus.SENT);
    }
}
