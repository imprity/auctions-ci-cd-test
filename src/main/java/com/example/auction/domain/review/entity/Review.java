package com.example.auction.domain.review.entity;

import com.example.auction.common.entity.ModifiableEntity;
import com.example.auction.common.exception.ServiceErrorException;
import com.example.auction.domain.review.dto.ReviewModifyRequest;
import com.example.auction.domain.review.exception.ReviewErrorEnum;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "reviews", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"auction_id", "reviewer_id"})
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review extends ModifiableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long auctionId;

    @Column(nullable = false)
    private Long reviewerId;

    @Column(nullable = false)
    private Long revieweeId;

    @Column(nullable = false)
    private int score;

    private String description;

    public static Review of(Long auctionId, Long reviewerId, Long revieweeId, int score, String description) {
        Review review = new Review();
        review.auctionId = auctionId;
        review.reviewerId = reviewerId;
        review.revieweeId = revieweeId;
        review.score = score;
        review.description = description;

        return review;
    }

    public void modify(ReviewModifyRequest request) {
        if (request.score() == null && request.description() == null) {
            throw new ServiceErrorException(ReviewErrorEnum.REVIEW_MODIFY_NO_CONTENT);
        }
        if (request.score() != null) {
            this.score = request.score();
        }
        if (request.description() != null) {
            this.description = request.description();
        }
    }
}
