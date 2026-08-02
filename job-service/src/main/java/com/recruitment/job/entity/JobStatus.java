package com.recruitment.job.entity;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Job posting lifecycle:
 * DRAFT -> PENDING_APPROVAL -> APPROVED -> PUBLISHED -> CLOSED
 * PENDING_APPROVAL can also be REJECTED (sent back to DRAFT-like state).
 */
public enum JobStatus {
    DRAFT,
    PENDING_APPROVAL,
    APPROVED,
    REJECTED,
    PUBLISHED,
    CLOSED;

    private static final Map<JobStatus, Set<JobStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(JobStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(DRAFT, EnumSet.of(PENDING_APPROVAL));
        ALLOWED_TRANSITIONS.put(PENDING_APPROVAL, EnumSet.of(APPROVED, REJECTED));
        ALLOWED_TRANSITIONS.put(REJECTED, EnumSet.of(DRAFT, PENDING_APPROVAL));
        ALLOWED_TRANSITIONS.put(APPROVED, EnumSet.of(PUBLISHED, CLOSED));
        ALLOWED_TRANSITIONS.put(PUBLISHED, EnumSet.of(CLOSED));
        ALLOWED_TRANSITIONS.put(CLOSED, EnumSet.noneOf(JobStatus.class));
    }

    public boolean canTransitionTo(JobStatus target) {
        return ALLOWED_TRANSITIONS.getOrDefault(this, Set.of()).contains(target);
    }
}
