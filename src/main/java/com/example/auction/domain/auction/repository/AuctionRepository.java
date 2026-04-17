package com.example.auction.domain.auction.repository;

import com.example.auction.domain.auction.enums.AuctionStatus;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.auction.domain.auction.entity.Auction;

import java.time.LocalDateTime;
import java.util.List;

public interface AuctionRepository extends
    JpaRepository<@NonNull Auction, @NonNull Long>, CustomAuctionRepository
{
    List<Auction> findAllByStatusAndStartedAtBefore(AuctionStatus auctionStatus, LocalDateTime now);

    List<Auction> findAllByStatusAndEndedAtBefore(AuctionStatus auctionStatus, LocalDateTime now);
    boolean existsByUserIdAndStatusIn(Long userId, List<AuctionStatus> auctionStatuses);
}
