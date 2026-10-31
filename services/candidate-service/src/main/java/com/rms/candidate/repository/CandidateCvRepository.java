package com.rms.candidate.repository;

import com.rms.candidate.entity.CandidateCv;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CandidateCvRepository extends JpaRepository<CandidateCv, Long> {
    List<CandidateCv> findByCandidateId(Long candidateId);
    Optional<CandidateCv> findByCandidateIdAndIsDefaultTrue(Long candidateId);
    long countByCandidateId(Long candidateId);
}
