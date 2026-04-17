package com.example.auction.domain.ai.tool;

import com.example.auction.domain.ai.tool.dto.AuctionBidInfo;
import com.example.auction.domain.ai.tool.dto.AuctionResultInfo;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.example.auction.domain.auction.entity.QAuction.auction;
import static com.example.auction.domain.auction.result.entity.QAuctionResult.auctionResult;
import static com.example.auction.domain.bid.entity.QBid.bid;
import static com.example.auction.domain.review.entity.QReview.review;

// AI Tool 전용 QueryDSL 구현체 — 다중 테이블 조인 및 AI 전용 집계 쿼리 처리
@Repository
@RequiredArgsConstructor
public class AiToolRepositoryImpl implements AiToolRepository {

    private static final int MAX_BIDS_FOR_TOOL = 50;

    private final JPAQueryFactory queryFactory;

    // bids 테이블 단순 조회 — 입찰가 오름차순 정렬로 최저가 우선 확인
    @Override
    public List<AuctionBidInfo> findBidsByAuctionId(Long auctionId) {
        return queryFactory
                .select(Projections.constructor(AuctionBidInfo.class,
                        bid.id,
                        bid.userId,
                        bid.price,
                        bid.createdAt
                ))
                .from(bid)
                .where(bid.auctionId.eq(auctionId))
                .orderBy(bid.price.asc(), bid.createdAt.asc(), bid.id.asc())
                .limit(MAX_BIDS_FOR_TOOL)
                .fetch();
    }

    // auction_results + auctions 조인 — 상품명으로 최근 낙찰가 이력 조회 (시세 파악용)
    @Override
    public List<AuctionResultInfo> findRecentAuctionResultsByItemName(String itemName) {
        return queryFactory
                .select(Projections.constructor(AuctionResultInfo.class,
                        auction.itemName,
                        auctionResult.price,
                        auction.endedAt
                ))
                .from(auctionResult)
                .join(auction).on(auctionResult.auctionId.eq(auction.id))
                .where(itemName != null && !itemName.isBlank()
                        ? auction.itemName.containsIgnoreCase(itemName.trim())
                        : null)
                .orderBy(auction.endedAt.desc())
                .limit(10)
                .fetch();
    }

    // auction_results 단순 집계 — 판매자의 총 낙찰 횟수 카운트
    @Override
    public long countSellerSales(Long sellerId) {
        Long count = queryFactory
                .select(auctionResult.count())
                .from(auctionResult)
                .where(auctionResult.sellerId.eq(sellerId))
                .fetchOne();
        return count != null ? count : 0L;
    }

    // reviews 단순 조회 — 판매자가 받은 최근 후기 텍스트만 추출 (LLM 분석용)
    @Override
    public List<String> findRecentReviewTextsBySellerId(Long sellerId) {
        return queryFactory
                .select(review.description)
                .from(review)
                .where(
                        review.revieweeId.eq(sellerId),
                        review.description.isNotNull(),  // 텍스트 없는 별점만 있는 후기 제외
                        Expressions.stringTemplate("trim({0})", review.description).ne("")  // 공백만 있는 후기 제외
                )
                .orderBy(review.createdAt.desc())
                .limit(5)
                .fetch();
    }
}