package com.rms.iam.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void registerRequestAccepts8CharValidPassword() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("user@example.com");
        req.setPassword("Pass123!");
        req.setRole("CANDIDATE");
        req.setPrivacyAccepted(true);

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(req);
        assertThat(violations).isEmpty();
    }

    @Test
    void registerRequestRejectsUnder8CharPassword() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("user@example.com");
        req.setPassword("Pass1!"); // 6 chars
        req.setRole("CANDIDATE");
        req.setPrivacyAccepted(true);

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(req);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }

    @Test
    void acceptInvitationRequestAccepts8CharValidPassword() {
        AcceptInvitationRequest req = new AcceptInvitationRequest();
        req.setToken("valid-token");
        req.setPassword("Pass123!");

        Set<ConstraintViolation<AcceptInvitationRequest>> violations = validator.validate(req);
        assertThat(violations).isEmpty();
    }

    @Test
    void acceptInvitationRequestRejectsUnder8CharPassword() {
        AcceptInvitationRequest req = new AcceptInvitationRequest();
        req.setToken("valid-token");
        req.setPassword("P1!abcd"); // 7 chars

        Set<ConstraintViolation<AcceptInvitationRequest>> violations = validator.validate(req);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }
}
