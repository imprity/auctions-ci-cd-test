package com.example.auction.domain.ai.tool;

import com.example.auction.domain.ai.tool.dto.AuctionBidInfo;
import com.example.auction.domain.ai.tool.dto.AuctionResultInfo;

import java.util.List;

// AI Tool 전용 QueryDSL 조회 인터페이스 (다중 도메인 데이터 조회)
public interface AiToolRepository {

    // 경매 ID로 입찰 목록 조회 (가격 오름차순)
    List<AuctionBidInfo> findBidsByAuctionId(Long auctionId);

    // 상품명으로 최근 낙찰 이력 조회 (최대 10건)
    List<AuctionResultInfo> findRecentAuctionResultsByItemName(String itemName);

    // 판매자의 총 낙찰 횟수 조회
    long countSellerSales(Long sellerId);

    // 판매자가 받은 최근 후기 텍스트 조회 (최대 5건)
    List<String> findRecentReviewTextsBySellerId(Long sellerId);
}
