package com.rms.iam.messaging.event;

import java.util.UUID;

public record CompanyRegistrationSubmittedEvent(UUID registrationId, UUID recipientUserId,
                                                String companyName, String applicantEmail) {
}
