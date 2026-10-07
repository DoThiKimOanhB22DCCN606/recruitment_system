package com.recruitment.candidate.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "candidate_portfolio_links")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CandidatePortfolioLink {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private Candidate candidate;

    private String title;
    @Column(nullable = false)
    private String url;
}
