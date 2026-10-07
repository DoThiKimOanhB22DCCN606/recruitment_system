package com.recruitment.candidate.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateCvResponse {
    private Long id;
    private String fileName;
    private Long fileSize;
    private String contentType;
    private boolean isDefault;
    private String downloadUrl; // presigned MinIO URL, time-limited
    private LocalDateTime uploadedAt;
}
