package com.moadams.authservice.dto;

public record AuthResponse(
        String accessToken,
        String tokenType,
        String refreshToken,
        Long expiresIn,
        String role
) {}
