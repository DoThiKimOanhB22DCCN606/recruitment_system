package com.rms.candidate.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** Used by the ATS Service internal API to verify a CV exists and fetch its download link. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InternalCvResponse {
    private Long cvId;
    private UUID candidateId;
    private String candidateName;
    private String candidateEmail;
    private String fileName;
    private String contentType;
    private String downloadUrl;
    private boolean valid;
}
