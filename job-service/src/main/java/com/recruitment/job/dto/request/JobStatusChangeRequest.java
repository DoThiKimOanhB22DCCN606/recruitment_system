package com.recruitment.job.dto.request;

import com.recruitment.job.entity.JobStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class JobStatusChangeRequest {

    @NotNull(message = "Target status is required")
    private JobStatus targetStatus;

    private String note;
}
