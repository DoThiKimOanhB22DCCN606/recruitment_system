package com.recruitment.candidate.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateExportResponse {
    private CandidateProfileResponse profile;
    private List<CandidateCvResponse> cvs;
    private Instant exportedAt;
}
