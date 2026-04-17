package com.example.auction.domain.review.dto;

import java.time.LocalDateTime;

public record ReviewModifyResponse(
        Long reviewId,
        int score,
        String description,
        LocalDateTime createdAt,
        LocalDateTime modifiedAt
) {}
