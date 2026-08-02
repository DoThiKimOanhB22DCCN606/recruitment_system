package com.recruitment.job.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "job_tags")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @Column(nullable = false)
    private String tag;
}
