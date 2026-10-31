package com.rms.ats.messaging;

import java.util.UUID;

public record CompanyRegistrationSubmittedEvent(UUID registrationId, UUID recipientUserId,
                                                String companyName, String applicantEmail) {
}
