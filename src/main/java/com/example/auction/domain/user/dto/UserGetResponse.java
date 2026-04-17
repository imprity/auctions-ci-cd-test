package com.example.auction.domain.user.dto;

import com.example.auction.domain.user.enums.UserRole;

import java.math.BigDecimal;

public record UserGetResponse(
        Long userId,
        String email,
        BigDecimal rating,
        UserRole role
) {}
