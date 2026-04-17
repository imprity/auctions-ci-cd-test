package com.example.auction.domain.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ReviewCreateRequest(
        @NotNull(message = "경매 식별자를 입력해주세요")
        Long auctionId,
        @Min(value = 1, message = "별점은 최소 1점입니다")
        @Max(value = 5, message = "별점은 최대 5점입니다")
        int score,
        String description
) {}
