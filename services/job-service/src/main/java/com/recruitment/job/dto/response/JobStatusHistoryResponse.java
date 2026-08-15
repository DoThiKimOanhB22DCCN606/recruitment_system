package com.recruitment.job.dto.response;

import com.recruitment.job.entity.JobStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobStatusHistoryResponse {
    private Long id;
    private JobStatus fromStatus;
    private JobStatus toStatus;
    private Long changedBy;
    private String note;
    private LocalDateTime changedAt;
}
