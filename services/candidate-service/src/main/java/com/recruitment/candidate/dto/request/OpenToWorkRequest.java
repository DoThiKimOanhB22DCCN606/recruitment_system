package com.recruitment.candidate.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OpenToWorkRequest {
    @NotNull
    private Boolean openToWork;

    /** Number of days the "open to work" status stays active before auto-expiring. */
    private Integer durationDays;
}
