package com.recruitment.candidate.repository;

import com.recruitment.candidate.entity.CandidateCv;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CandidateCvRepository extends JpaRepository<CandidateCv, Long> {
    List<CandidateCv> findByCandidateId(Long candidateId);
    Optional<CandidateCv> findByCandidateIdAndIsDefaultTrue(Long candidateId);
}
