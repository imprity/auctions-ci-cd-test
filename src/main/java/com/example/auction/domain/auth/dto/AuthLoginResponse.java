package com.example.auction.domain.auth.dto;

public record AuthLoginResponse(
        String accessToken,
        String refreshToken
) {}
