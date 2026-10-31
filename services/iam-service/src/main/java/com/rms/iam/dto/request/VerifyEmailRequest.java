package com.rms.iam.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class VerifyEmailRequest {
    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Pattern(regexp = "^\\d{6}$", message = "OTP must contain exactly 6 digits")
    private String otp;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getOtp() { return otp; }
    public void setOtp(String otp) { this.otp = otp; }
}
