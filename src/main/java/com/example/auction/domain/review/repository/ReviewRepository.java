package com.example.auction.domain.review.repository;

import com.example.auction.domain.review.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, Long>, ReviewCustomRepository {
    boolean existsByAuctionIdAndReviewerId(Long auctionId, Long reviewerId);

    @Query("SELECT AVG(r.score) FROM Review r WHERE r.revieweeId = :revieweeId")
    Double findAvgScoreByRevieweeId(@Param("revieweeId") Long revieweeId);
}
