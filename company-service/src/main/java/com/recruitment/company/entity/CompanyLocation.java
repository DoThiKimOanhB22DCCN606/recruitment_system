package com.recruitment.company.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "company_locations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CompanyLocation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(nullable = false)
    private String address;
    private String city;
    private String country;

    @Column(name = "is_headquarters")
    private boolean headquarters;
}
