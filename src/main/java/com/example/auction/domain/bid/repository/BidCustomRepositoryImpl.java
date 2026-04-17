package com.example.auction.domain.bid.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class BidCustomRepositoryImpl implements BidCustomRepository {

    private final JPAQueryFactory queryFactory;
}
