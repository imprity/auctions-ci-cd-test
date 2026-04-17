package com.example.auction.domain.bid.repository;

import com.example.auction.domain.bid.entity.Bid;
import com.example.auction.domain.bid.enums.BidAuctionStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface BidRepository extends JpaRepository<Bid, Long>, BidCustomRepository {
    Optional<Bid> findFirstByAuctionIdOrderByPriceAsc(Long auctionId);

    Page<Bid> findAllByUserId(Long userId, Pageable pageable);

    Page<Bid> findAllByAuctionId(Long auctionId, Pageable pageable);

    @Query("SELECT MIN(b.price) FROM Bid b WHERE b.auctionId = :auctionId")
    Optional<BigDecimal> findMinPriceByAuctionId(@Param("auctionId") Long auctionId);

    boolean existsByUserIdAndStatus(Long userId, BidAuctionStatus bidAuctionStatus);
}
