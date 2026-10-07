package com.recruitment.company.dto.response;

/** Used by other services (Job, ATS) to resolve basic company info. */
public record InternalCompanyResponse(
        Long id,
        String name,
        String logoUrl,
        boolean verified
) {}
