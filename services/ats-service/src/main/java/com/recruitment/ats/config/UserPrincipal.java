package com.recruitment.ats.config;

import java.util.UUID;

public record UserPrincipal(UUID userId, UUID tenantId, String role) {
}
