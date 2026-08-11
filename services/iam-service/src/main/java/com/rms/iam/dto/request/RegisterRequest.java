package com.rms.iam.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.AssertTrue;

public class RegisterRequest {
    @NotBlank
    @Email
    private String email;

    @NotBlank
    // AUDIT FIX FINDING-01: Password Complexity
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{12,}$",
             message = "Password must be at least 12 characters and include letters, numbers, and special characters")
    @com.fasterxml.jackson.annotation.JsonProperty(access = com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY)
    private String password;

    private String companyName;

    @NotBlank
    private String role; // "HR_ADMIN" or "CANDIDATE"

    @NotNull
    @AssertTrue(message = "Privacy policy must be accepted")
    private Boolean privacyAccepted;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Boolean getPrivacyAccepted() { return privacyAccepted; }
    public void setPrivacyAccepted(Boolean privacyAccepted) { this.privacyAccepted = privacyAccepted; }
}
