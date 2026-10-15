package com.gahrms.backend.modules.identity.dto;

public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private long expiresInMs;
    private UserSummaryDto user;

    public AuthResponse() {}

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final AuthResponse res = new AuthResponse();
        public Builder accessToken(String token) { res.accessToken = token; return this; }
        public Builder refreshToken(String token) { res.refreshToken = token; return this; }
        public Builder tokenType(String type) { res.tokenType = type; return this; }
        public Builder expiresInMs(long ms) { res.expiresInMs = ms; return this; }
        public Builder user(UserSummaryDto user) { res.user = user; return this; }
        public AuthResponse build() { return res; }
    }

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }

    public long getExpiresInMs() { return expiresInMs; }
    public void setExpiresInMs(long expiresInMs) { this.expiresInMs = expiresInMs; }

    public UserSummaryDto getUser() { return user; }
    public void setUser(UserSummaryDto user) { this.user = user; }
}
