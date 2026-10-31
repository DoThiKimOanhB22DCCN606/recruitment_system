package com.rms.interview.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.rms.interview.entity.InterviewFeedback;

public interface InterviewFeedbackRepository extends JpaRepository<InterviewFeedback, UUID> {
    Optional<InterviewFeedback> findByInterviewIdAndReviewerId(UUID interviewId, UUID reviewerId);

    List<InterviewFeedback> findByInterviewIdAndTenantIdOrderByCreatedAtDesc(UUID interviewId, UUID tenantId);
}
