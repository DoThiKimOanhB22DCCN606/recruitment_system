package com.rms.iam.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class AuthResponse {
    private String accessToken;
    @JsonIgnore
    private String refreshToken;
    private UserDto user;

    public AuthResponse(String accessToken, String refreshToken, UserDto user) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.user = user;
    }

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

    public UserDto getUser() { return user; }
    public void setUser(UserDto user) { this.user = user; }
}
