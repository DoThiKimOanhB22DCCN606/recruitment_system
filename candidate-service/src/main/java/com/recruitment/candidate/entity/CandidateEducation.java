package com.recruitment.candidate.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "candidate_educations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CandidateEducation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private Candidate candidate;

    @Column(name = "school_name", nullable = false)
    private String schoolName;
    private String degree;
    @Column(name = "field_of_study")
    private String fieldOfStudy;
    @Column(name = "start_date")
    private LocalDate startDate;
    @Column(name = "end_date")
    private LocalDate endDate;
    @Column(columnDefinition = "TEXT")
    private String description;
}
