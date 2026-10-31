package com.rms.interview.dto;

import java.time.Instant;
import java.util.UUID;

import com.rms.interview.entity.InterviewFeedback;
import com.rms.interview.entity.InterviewRecommendation;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class InterviewFeedbackDtos {

    private InterviewFeedbackDtos() {
    }

    public record FeedbackRequest(
            @NotNull @Min(1) @Max(5) Integer score,
            @NotBlank @Size(max = 2000) String feedback,
            @NotNull InterviewRecommendation recommendation) {
    }

    public record FeedbackResponse(
            UUID id,
            UUID interviewId,
            UUID reviewerId,
            Integer score,
            String feedback,
            InterviewRecommendation recommendation,
            Instant createdAt,
            Instant updatedAt) {

        public static FeedbackResponse from(InterviewFeedback feedback) {
            return new FeedbackResponse(
                    feedback.getId(), feedback.getInterviewId(), feedback.getReviewerId(),
                    feedback.getScore(), feedback.getFeedback(), feedback.getRecommendation(),
                    feedback.getCreatedAt(), feedback.getUpdatedAt());
        }
    }
}
