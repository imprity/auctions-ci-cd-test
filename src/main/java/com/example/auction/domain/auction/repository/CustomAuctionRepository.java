package com.example.auction.domain.auction.repository;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;

import com.example.auction.domain.auction.dto.AuctionSearchCondition;
import com.example.auction.domain.auction.entity.Auction;

public interface CustomAuctionRepository {
    Page<@NonNull Auction> findByCondition(AuctionSearchCondition condition);
    Page<@NonNull Auction> findByUserIdAndCondition(Long userId, AuctionSearchCondition condition);
}
