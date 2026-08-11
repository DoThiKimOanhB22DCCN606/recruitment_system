package com.rms.iam.controller;

import com.rms.iam.dto.request.*;
import com.rms.iam.dto.response.AuthResponse;
import com.rms.iam.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public void register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        AuthResponse authResponse = authService.login(request);
        
        ResponseCookie cookie = ResponseCookie.from("rms_refresh_token", authResponse.getRefreshToken())
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/api/v1/auth/refresh")
                .maxAge(7 * 24 * 60 * 60) // 7 days
                .build();
                
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        
        return authResponse;
    }

    @PostMapping("/verify-email")
    @ResponseStatus(HttpStatus.OK)
    public void verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        authService.verifyEmail(request);
    }

    @PostMapping("/resend-verification")
    @ResponseStatus(HttpStatus.OK)
    public void resendVerification(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.resendVerification(request.getEmail());
    }

    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.OK)
    public void forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
    }

    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.OK)
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
    }

    @PostMapping("/refresh")
    @ResponseStatus(HttpStatus.OK)
    public AuthResponse refresh(@CookieValue(name = "rms_refresh_token", required = false) String refreshToken, HttpServletResponse response) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new com.rms.common.exception.RmsException(com.rms.common.exception.ErrorCode.ERR_MISSING_CREDENTIALS, "Refresh token is missing");
        }
        
        AuthResponse authResponse = authService.refreshToken(refreshToken);
        
        ResponseCookie cookie = ResponseCookie.from("rms_refresh_token", authResponse.getRefreshToken())
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/api/v1/auth/refresh")
                .maxAge(7 * 24 * 60 * 60) // 7 days
                .build();
                
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        
        return authResponse;
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@CookieValue(name = "rms_refresh_token", required = false) String refreshToken,
                       @RequestHeader(value = "X-User-Id", required = false) java.util.UUID userId,
                       HttpServletResponse response) {
        
        // Since Gateway strips Authorization, we might not have it here to extract TTL.
        // We pass the rawRefreshToken and userId to the service.
        if (userId != null) {
            authService.logout(null, refreshToken, userId);
        }

        // Clear the refresh token cookie
        ResponseCookie cookie = ResponseCookie.from("rms_refresh_token", "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/api/v1/auth/refresh")
                .maxAge(0) // Expire immediately
                .build();
                
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
