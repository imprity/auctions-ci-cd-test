package com.example.auction.domain.ai.tool;

import com.example.auction.domain.ai.service.ReviewEmbeddingService;
import com.example.auction.domain.ai.tool.dto.AuctionBidInfo;
import com.example.auction.domain.ai.tool.dto.AuctionResultInfo;
import com.example.auction.domain.ai.tool.dto.SellerStatsInfo;
import com.example.auction.domain.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.List;

// LLM이 호출할 수 있는 AI Tool 모음 — 경매 데이터 조회 기능 4가지 제공
@Component
@RequiredArgsConstructor
public class AuctionTools {

    private final AiToolRepository aiToolRepository;
    private final ReviewRepository reviewRepository;         // 평균 평점 조회 (이미 구현된 JPQL 활용)
    private final ReviewEmbeddingService reviewEmbeddingService; // RAG 유사도 검색

    // 특정 경매의 입찰 목록과 최저가를 조회 — 경쟁 입찰 분석에 활용
    @Tool(description = "특정 경매 ID로 입찰 목록을 조회합니다. 입찰가 오름차순으로 정렬되어 최저가를 확인할 수 있습니다.")
    public List<AuctionBidInfo> getBidsByAuctionId(Long auctionId) {
        if (auctionId == null) {
            throw new IllegalArgumentException("auctionId는 필수입니다.");
        }
        return aiToolRepository.findBidsByAuctionId(auctionId);
    }

    // 상품명으로 최근 낙찰 이력을 조회 — 시세 파악에 활용
    @Tool(description = "상품명으로 최근 낙찰 이력을 조회합니다. 유사 상품의 시세 파악에 활용됩니다.")
    public List<AuctionResultInfo> getRecentAuctionResults(String itemName) {
        return aiToolRepository.findRecentAuctionResultsByItemName(itemName);
    }

    // 판매자 후기 의미 검색 — RAG 기반으로 질문과 관련 있는 후기 텍스트 반환
    @Tool(description = "판매자 ID로 질문과 의미적으로 유사한 후기를 검색합니다. 판매자 신뢰도 심층 분석에 활용됩니다.")
    public List<String> getSellerReviewInsights(Long sellerId, String query) {
        if (sellerId == null) {
            throw new IllegalArgumentException("sellerId는 필수입니다.");
        }
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("query는 필수입니다.");
        }
        return reviewEmbeddingService.search(sellerId, query);
    }

    // 판매자의 낙찰 횟수, 평균 평점, 최근 후기를 종합 조회 — 판매자 신뢰도 분석에 활용
    @Tool(description = "판매자 ID로 총 낙찰 횟수, 평균 평점, 최근 후기를 조회합니다. 판매자 신뢰도 분석에 활용됩니다.")
    public SellerStatsInfo getSellerStats(Long sellerId) {
        if (sellerId == null) {
            throw new IllegalArgumentException("sellerId는 필수입니다.");
        }
        long totalSales = aiToolRepository.countSellerSales(sellerId);
        Double avgScore = reviewRepository.findAvgScoreByRevieweeId(sellerId); // 리뷰 없으면 null
        List<String> recentReviews = aiToolRepository.findRecentReviewTextsBySellerId(sellerId);

        return new SellerStatsInfo(
                sellerId,
                totalSales,
                avgScore != null ? avgScore : 0.0,  // 리뷰 없는 판매자는 0.0 처리
                recentReviews
        );
    }
}