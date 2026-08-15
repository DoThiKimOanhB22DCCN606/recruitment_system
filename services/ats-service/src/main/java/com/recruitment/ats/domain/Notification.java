package com.recruitment.ats.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "notifications")
public class Notification {
    @Id
    @GeneratedValue
    private UUID id;
    private UUID tenantId;
    private UUID recipientUserId;
    private String actionType;
    private String entityType;
    private UUID entityId;
    @Column(length = 300)
    private String title;
    @Column(length = 1000)
    private String message;
    private boolean read;
    private Instant createdAt = Instant.now();
    private Instant readAt;
}
