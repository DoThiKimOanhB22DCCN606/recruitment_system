package com.recruitment.candidate.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "candidate_experiences")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CandidateExperience {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private Candidate candidate;

    @Column(name = "company_name", nullable = false)
    private String companyName;
    @Column(name = "job_title", nullable = false)
    private String jobTitle;
    @Column(name = "start_date")
    private LocalDate startDate;
    @Column(name = "end_date")
    private LocalDate endDate;
    @Column(name = "is_current")
    private boolean current;
    @Column(columnDefinition = "TEXT")
    private String description;
}
