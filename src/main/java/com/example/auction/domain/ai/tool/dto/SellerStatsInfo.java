package com.example.auction.domain.ai.tool.dto;

import java.util.List;

// AI Tool이 LLM에 반환하는 판매자 통계 정보 (getSellerStats 반환 타입)
public record SellerStatsInfo(
        Long sellerId,              // 판매자 ID
        long totalSales,            // 총 낙찰 횟수
        double avgScore,            // 평균 평점 (리뷰 없으면 0.0)
        List<String> recentReviews  // 최근 후기 텍스트 목록 (최대 5개)
) {}