package com.example.auction.domain.review.dto;

import java.time.LocalDateTime;

public record ReviewListGetResponse(
        Long reviewId,
        Long auctionId,
        Long counterpartId,
        LocalDateTime createdAt,
        LocalDateTime modifiedAt
) {}
