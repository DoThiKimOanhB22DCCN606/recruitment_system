package com.recruitment.candidate.repository;

import com.recruitment.candidate.entity.Candidate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CandidateRepository extends JpaRepository<Candidate, Long> {
    Optional<Candidate> findByUserUuid(java.util.UUID userUuid);
    Optional<Candidate> findByUserId(Long userId);

    List<Candidate> findByOpenToWorkTrueAndOpenToWorkUntilBefore(LocalDateTime now);

    List<Candidate> findByOpenToWorkTrueAndOpenToWorkUntilBetween(LocalDateTime start, LocalDateTime end);

    List<Candidate> findByOpenToWorkTrueAndOpenToWorkUntilBetweenAndWarningSentAtIsNull(LocalDateTime start, LocalDateTime end);
}
