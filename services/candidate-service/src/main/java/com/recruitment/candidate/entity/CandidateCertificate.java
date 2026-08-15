package com.recruitment.candidate.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "candidate_certificates")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CandidateCertificate {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private Candidate candidate;

    private String name;
    @Column(name = "issued_by")
    private String issuedBy;
    @Column(name = "issued_date")
    private LocalDate issuedDate;
    @Column(name = "expiry_date")
    private LocalDate expiryDate;
    @Column(name = "credential_url")
    private String credentialUrl;
}
