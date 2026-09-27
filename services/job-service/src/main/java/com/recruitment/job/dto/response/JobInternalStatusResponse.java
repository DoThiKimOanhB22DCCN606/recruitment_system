package com.recruitment.job.dto.response;

import com.recruitment.job.entity.JobStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Used by the internal API consumed by the ATS Service. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobInternalStatusResponse {
    private Long id;
    private Long jobId;
    private java.util.UUID tenantId;
    private java.util.UUID recruiterId;
    private JobStatus status;
    private boolean acceptingApplications;

    public Long getId() {
        return id != null ? id : jobId;
    }
}
