package com.rms.iam.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AcceptInvitationRequest {

    @NotBlank(message = "Token is required")
    private String token;

    @NotBlank(message = "Password is required")
    @jakarta.validation.constraints.Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$",
             message = "Password must be at least 8 characters and include letters, numbers, and special characters")
    private String password;

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
