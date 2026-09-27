package com.recruitment.ats.config;

import java.util.UUID;

public final class RequestContext {

    private static final ThreadLocal<Identity> CURRENT = new ThreadLocal<>();

    private RequestContext() {
    }

    public static void set(UUID userId, UUID tenantId, String role) {
        set(userId, tenantId, role, null);
    }

    public static void set(UUID userId, UUID tenantId, String role, String userName) {
        String displayName = (userName != null && !userName.isBlank()) ? userName : (role != null ? formatRole(role) : "System");
        CURRENT.set(new Identity(userId, tenantId, role == null ? "USER" : role, displayName));
    }

    public static UUID userId() {
        return required().userId();
    }

    public static UUID currentUserId() {
        Identity identity = CURRENT.get();
        return identity != null ? identity.userId() : null;
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

    public static String userName() {
        Identity identity = CURRENT.get();
        return identity != null ? identity.userName() : "System";
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

    private static String formatRole(String role) {
        if ("HR_ADMIN".equalsIgnoreCase(role)) return "HR Admin";
        if ("RECRUITER".equalsIgnoreCase(role)) return "Recruiter";
        if ("CANDIDATE".equalsIgnoreCase(role)) return "Candidate";
        if ("SYS_ADMIN".equalsIgnoreCase(role)) return "System Admin";
        return role;
    }

    private record Identity(UUID userId, UUID tenantId, String role, String userName) {
    }
}
