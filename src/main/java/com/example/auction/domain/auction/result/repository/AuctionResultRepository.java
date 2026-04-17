package com.example.auction.domain.auction.result.repository;

import com.example.auction.domain.auction.result.entity.AuctionResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuctionResultRepository extends JpaRepository<AuctionResult, Long> {
    Optional<AuctionResult> findByAuctionId(Long auctionId);
}
