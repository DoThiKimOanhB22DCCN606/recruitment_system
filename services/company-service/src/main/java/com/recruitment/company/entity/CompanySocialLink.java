package com.recruitment.company.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "company_social_links")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CompanySocialLink {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(nullable = false)
    private String platform;

    @Column(nullable = false)
    private String url;
}
