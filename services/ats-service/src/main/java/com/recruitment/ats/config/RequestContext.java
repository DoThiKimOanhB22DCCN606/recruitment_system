package com.recruitment.ats.config;

import java.util.UUID;

public final class RequestContext {

    private static final ThreadLocal<Identity> CURRENT = new ThreadLocal<>();

    private RequestContext() {
    }

    public static void set(UUID userId, UUID tenantId, String role) {
        CURRENT.set(new Identity(userId, tenantId, role == null ? "USER" : role));
    }

    public static UUID userId() {
        return required().userId();
    }

    public static UUID tenantId() {
        return required().tenantId();
    }

    public static UUID requireTenantId() {
        UUID tenantId = tenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant context is required for this operation");
        }
        return tenantId;
    }

    public static String role() {
        return required().role();
    }

    public static void clear() {
        CURRENT.remove();
    }

    private static Identity required() {
        Identity identity = CURRENT.get();
        if (identity == null) {
            throw new IllegalStateException("Request identity is unavailable");
        }
        return identity;
    }

    private record Identity(UUID userId, UUID tenantId, String role) {
    }
}
