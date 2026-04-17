package com.example.auction.domain.auction.repository;
import static com.example.auction.domain.auction.entity.QAuction.auction;

import com.example.auction.domain.auction.dto.AuctionSearchCondition;
import com.example.auction.domain.auction.entity.Auction;
import com.example.auction.domain.auction.enums.AuctionStatus;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class CustomAuctionRepositoryImpl implements CustomAuctionRepository{

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<@NonNull Auction> findByCondition(
            AuctionSearchCondition condition
    ) {
        return findByConditionImpl(null, condition);
    }

    @Override
    public Page<@NonNull Auction> findByUserIdAndCondition(
            Long userId,
            AuctionSearchCondition condition
    ) {
        return findByConditionImpl(userId, condition);
    }


    private Page<@NonNull Auction> findByConditionImpl(
            @Nullable Long userId,
            AuctionSearchCondition condition
    ) {
        PageRequest pageRequest = PageRequest.of(
            condition.getPage(),
            condition.getPageSize()
        );

        BooleanExpression[] booleans = new BooleanExpression[] {
                statusContains(condition),
                inMaxPriceRange(condition),
                hasCategory(condition),
                isOwnedBy(userId),
                hasKeyword(condition)
        };

        List<Auction> auctions = queryFactory
            .selectFrom(auction)
            .where(booleans)
            .orderBy(new OrderSpecifier<>(Order.DESC, auction.createdAt))
            .offset(pageRequest.getOffset())
            .limit(condition.getPageSize())
            .fetch();

        JPAQuery<Long> totalCount = queryFactory
            .select(auction.count())
            .from(auction)
            .where(booleans);

        return PageableExecutionUtils.getPage(auctions, pageRequest, () -> totalCount.fetchOne());
    }

    private BooleanExpression statusContains(AuctionSearchCondition condition) {
        Set<AuctionStatus> statuses = condition.getStatus();

        if (statuses != null && !statuses.isEmpty()) {
            return auction.status.in(statuses);
        }

        return null;
    }

    private BooleanExpression inMaxPriceRange(AuctionSearchCondition condition) {
         if(condition.getMaxPriceMin() != null && condition.getMaxPriceMax() != null) {

            return auction.maxPrice.between(condition.getMaxPriceMin(), condition.getMaxPriceMax());

         } else if (condition.getMaxPriceMin() != null) {

            return auction.maxPrice.goe(condition.getMaxPriceMin());

         } else if (condition.getMaxPriceMax() != null) {

            return auction.maxPrice.loe(condition.getMaxPriceMax());

         } else {
            return null;
         }
    }

    private BooleanExpression hasCategory(AuctionSearchCondition condition) {
        if (condition.getCategory() != null) {
            return auction.category.eq(condition.getCategory());
        }

        return null;
    }

    private BooleanExpression isOwnedBy(@Nullable Long userId) {
        if (userId != null) {
            return auction.userId.eq(userId);
        }

        return null;
    }

    private BooleanExpression hasKeyword(AuctionSearchCondition condition) {
        // TODO: 일단 간단한 like 키워드로만 검색
        if (condition.getKeyword() != null) {
            return auction.itemName.likeIgnoreCase(condition.getKeyword());
        }

        return null;
    }
}
