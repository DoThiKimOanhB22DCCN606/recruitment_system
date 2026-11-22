package com.rms.interview.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.LocalDate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.rms.interview.entity.Interview;

public interface InterviewRepository extends JpaRepository<Interview, UUID> {
    Optional<Interview> findByIdAndTenantId(UUID id, UUID tenantId);
    List<Interview> findByTenantIdOrderByScheduledDateAscStartTimeAsc(UUID tenantId);
    List<Interview> findByApplicationIdAndTenantIdOrderByScheduledDateAscStartTimeAsc(
            UUID applicationId, UUID tenantId);
    List<Interview> findByApplicationIdInOrderByScheduledDateAscStartTimeAsc(Iterable<UUID> applicationIds);
    List<Interview> findByCandidateIdAndStatusAndScheduledDate(UUID candidateId,
            com.rms.interview.entity.InterviewStatus status, LocalDate scheduledDate);
    List<Interview> findByCandidateIdAndStatusAndScheduledDateBetween(UUID candidateId,
            com.rms.interview.entity.InterviewStatus status, LocalDate from, LocalDate to);

    @Query(value = """
            SELECT i.*
            FROM interviews i
            JOIN ats_db.applications a ON a.id = i.application_id
            WHERE COALESCE(i.candidate_id, a.candidate_id) = :candidateId
              AND i.status = :status
              AND i.scheduled_date BETWEEN :fromDate AND :toDate
            """, nativeQuery = true)
    List<Interview> findPotentialConflicts(
            @Param("candidateId") UUID candidateId,
            @Param("status") String status,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);
}
