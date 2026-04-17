package com.example.auction.domain.ai.tool.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// AI Tool이 LLM에 반환하는 입찰 정보 (getBidsByAuctionId 반환 타입)
public record AuctionBidInfo(
        Long bidId,             // 입찰 ID
        Long sellerId,          // 판매자 ID
        BigDecimal price,       // 입찰가 (DB DECIMAL → BigDecimal)
        LocalDateTime createdAt // 입찰 시각
) {}
