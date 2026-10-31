package com.rms.gateway.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private final String secret = "1234567890123456789012345678901234567890";
    private final JwtUtil jwtUtil = new JwtUtil(secret);

    @Test
    void parseValidTokenReturnsClaims() {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();

        String token = Jwts.builder()
                .subject(userId.toString())
                .claim("tenantId", tenantId.toString())
                .claim("role", "HR_ADMIN")
                .expiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(key)
                .compact();

        Claims claims = jwtUtil.getClaims(token);
        assertThat(claims.getSubject()).isEqualTo(userId.toString());
        assertThat(claims.get("tenantId", String.class)).isEqualTo(tenantId.toString());
        assertThat(claims.get("role", String.class)).isEqualTo("HR_ADMIN");
    }
}