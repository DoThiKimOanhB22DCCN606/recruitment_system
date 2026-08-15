package com.recruitment.ats.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "idempotency_keys")
public class IdempotencyKey {
    @Id
    private String id;
    private String scope;
    private UUID resourceId;
    private Instant createdAt = Instant.now();

    public IdempotencyKey(String id, String scope, UUID resourceId) {
        this.id = id;
        this.scope = scope;
        this.resourceId = resourceId;
    }
}
