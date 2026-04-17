package com.example.auction.domain.auth.dto;

import com.example.auction.domain.user.enums.UserRole;

import java.time.LocalDateTime;

public record AuthSignupResponse(
        Long userId,
        String email,
        UserRole role,
        LocalDateTime createdAt
) {}
