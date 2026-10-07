package com.recruitment.candidate.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "candidate_languages")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CandidateLanguage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private Candidate candidate;

    private String language;
    private String proficiency;
}
