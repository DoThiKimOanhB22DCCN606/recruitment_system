package com.rms.iam.service;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        // Mock constructor params
        jwtTokenProvider = new JwtTokenProvider("testSecretKeyWhichIsAtLeast32BytesLongForHS256Algorithm", 900L); 
    }

    @Test
    void testGenerateToken() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        String token = jwtTokenProvider.generateAccessToken(
                userId, 
                "SYS_ADMIN", 
                tenantId,
                "admin@rms.com"
        );

        assertNotNull(token);
    }
}
