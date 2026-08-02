package com.recruitment.candidate.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Used by the ATS Service internal API to verify a CV exists and fetch its download link. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InternalCvResponse {
    private Long cvId;
    private Long candidateId;
    private String fileName;
    private String downloadUrl;
    private boolean valid;
}
