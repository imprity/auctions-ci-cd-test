package com.example.auction.domain.auction.dto;

import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.auction.domain.auction.entity.Auction;
import com.example.auction.domain.auction.enums.AuctionProductCategory;
import com.example.auction.domain.auction.enums.AuctionStatus;

import lombok.AllArgsConstructor;

@Getter
@AllArgsConstructor
public class GetAuctionResponse {
    private final Long id;

    private final Long userId;

    private final String description;

    private final BigDecimal maxPrice;

    private final String itemName;

    private final AuctionStatus status;

    private final LocalDateTime startedAt;
    private final LocalDateTime endedAt;
    private final LocalDateTime cancelledAt;

    private final AuctionProductCategory category;

    private final LocalDateTime createdAt;

    public static GetAuctionResponse from(Auction auction) {
        return new GetAuctionResponse (
            auction.getId(),

            auction.getUserId(),

            auction.getDescription(),

            auction.getMaxPrice(),

            auction.getItemName(),

            auction.getStatus(),

            auction.getStartedAt(),
            auction.getEndedAt(),
            auction.getCancelledAt(),

            auction.getCategory(),

            auction.getCreatedAt()
        );
    }
}
