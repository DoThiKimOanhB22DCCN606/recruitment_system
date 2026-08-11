package com.recruitment.interview.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "interviews")
public class Interview {
    @Id
    @GeneratedValue
    private UUID id;
    private UUID applicationId;
    private UUID tenantId;
    private LocalDate scheduledDate;
    private LocalTime startTime;
    private int durationMinutes;
    @Enumerated(EnumType.STRING)
    private InterviewType interviewType;
    @Column(length = 500)
    private String locationOrUrl;
    @Column(length = 2000)
    private String notes;
    @Enumerated(EnumType.STRING)
    private InterviewStatus status = InterviewStatus.SCHEDULED;
    private UUID createdBy;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "interview_participants", joinColumns = @JoinColumn(name = "interview_id"))
    @Column(name = "user_id")
    private Set<UUID> participantIds = new LinkedHashSet<>();
}
