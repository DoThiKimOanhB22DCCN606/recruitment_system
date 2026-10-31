package com.rms.iam.messaging.event;

public record EmailEvent(String email, String token, String type) {}
