package com.rms.iam.service;

import com.rms.iam.dto.request.LoginRequest;
import com.rms.iam.dto.request.RegisterRequest;
import com.rms.iam.dto.response.AuthResponse;
import com.rms.iam.entity.User;
import com.rms.iam.entity.UserStatus;
import com.rms.iam.repository.EmailVerificationTokenRepository;
import com.rms.iam.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.containers.GenericContainer;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
public class AuthServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @Container
    static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3-management-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
        registry.add("spring.rabbitmq.host", rabbitmq::getHost);
        registry.add("spring.rabbitmq.port", rabbitmq::getAmqpPort);
    }

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

    @AfterEach
    void cleanUp() {
        emailVerificationTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void testRegistrationFlow() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("integration@test.com");
        req.setPassword("Valid1@Password!");
        req.setRole("CANDIDATE");
        req.setPrivacyAccepted(true);

        authService.register(req);

        Optional<User> savedUser = userRepository.findByEmail("integration@test.com");
        assertThat(savedUser).isPresent();
        assertThat(savedUser.get().getStatus()).isEqualTo(UserStatus.PENDING_VERIFICATION);
        assertThat(emailVerificationTokenRepository.count()).isEqualTo(1);
    }
}
