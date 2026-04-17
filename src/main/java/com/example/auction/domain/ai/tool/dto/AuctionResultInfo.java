package com.example.auction.domain.ai.tool.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// AI Tool이 LLM에 반환하는 낙찰 이력 정보 (getRecentAuctionResults 반환 타입)
public record AuctionResultInfo(
        String itemName,          // 상품명
        BigDecimal price,         // 낙찰가
        LocalDateTime endedAt     // 경매 종료 시각
) {}