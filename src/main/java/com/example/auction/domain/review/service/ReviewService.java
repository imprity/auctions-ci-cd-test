package com.example.auction.domain.review.service;

import com.example.auction.common.dto.PageResponse;
import com.example.auction.common.exception.ServiceErrorException;
import com.example.auction.domain.ai.service.ReviewEmbeddingService;
import com.example.auction.domain.auction.result.entity.AuctionResult;
import com.example.auction.domain.auction.result.exception.AuctionResultErrorEnum;
import com.example.auction.domain.auction.result.repository.AuctionResultRepository;
import com.example.auction.domain.review.dto.*;
import com.example.auction.domain.review.entity.Review;
import com.example.auction.domain.review.exception.ReviewErrorEnum;
import com.example.auction.domain.review.repository.ReviewRepository;
import com.example.auction.domain.user.entity.User;
import com.example.auction.domain.user.exception.UserErrorEnum;
import com.example.auction.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final AuctionResultRepository auctionResultRepository;
    private final ReviewEmbeddingService reviewEmbeddingService;

    @Transactional
    public ReviewCreateResponse createReview(Long userId, ReviewCreateRequest request) {
        if (reviewRepository.existsByAuctionIdAndReviewerId(request.auctionId(), userId)) {
            throw new ServiceErrorException(ReviewErrorEnum.ALREADY_REVIEWED);
        }

        AuctionResult auctionResult = auctionResultRepository.findByAuctionId(request.auctionId()).orElseThrow(
                () -> new ServiceErrorException(AuctionResultErrorEnum.AUCTION_RESULT_NOT_FOUND));

        Long revieweeId;
        if (auctionResult.getBuyerId().equals(userId)) {
            revieweeId = auctionResult.getSellerId();
        } else if (auctionResult.getSellerId().equals(userId)) {
            revieweeId = auctionResult.getBuyerId();
        } else {
            throw new ServiceErrorException(ReviewErrorEnum.REVIEW_NOT_ALLOWED);
        }

        User reviewee = userRepository.findByIdAndDeletedFalse(revieweeId).orElseThrow(
                () -> new ServiceErrorException(UserErrorEnum.USER_NOT_FOUND));

        Review review = Review.of(request.auctionId(), userId, reviewee.getId(), request.score(), request.description());
        try {
            reviewRepository.save(review);
        } catch (DataIntegrityViolationException e) {
            throw new ServiceErrorException(ReviewErrorEnum.ALREADY_REVIEWED);
        }
        try {
            reviewEmbeddingService.embed(review); // 후기 텍스트 pgvector 임베딩 저장 (RAG용)
        } catch (Exception e) {
            log.warn("[ReviewService] 임베딩 저장 실패 — 리뷰 생성은 정상 처리됨. reviewId={}, error={}", review.getId(), e.getMessage());
        }

        Double avgScore = reviewRepository.findAvgScoreByRevieweeId(reviewee.getId());
        BigDecimal rating = BigDecimal.valueOf(avgScore).setScale(1, RoundingMode.HALF_UP);
        reviewee.updateRating(rating);

        return new ReviewCreateResponse(
                review.getId(),
                review.getAuctionId(),
                review.getReviewerId(),
                review.getRevieweeId(),
                review.getScore(),
                review.getDescription(),
                review.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewListGetResponse> getWrittenReviewList(Long userId, ReviewSearchCondition condition) {
        Page<ReviewListGetResponse> reviewList = reviewRepository.findWrittenReviewsWithConditions(
                userId,
                PageRequest.of(condition.getPage(), condition.getSize()),
                condition.getStartDate(),
                condition.getEndDate()
        );

        return PageResponse.create(reviewList);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewListGetResponse> getReceivedReviewList(Long userId, ReviewSearchCondition condition) {
        Page<ReviewListGetResponse> reviewList = reviewRepository.findReceivedReviewsWithConditions(
                userId,
                PageRequest.of(condition.getPage(), condition.getSize()),
                condition.getStartDate(),
                condition.getEndDate()
        );

        return PageResponse.create(reviewList);
    }

    @Transactional(readOnly = true)
    public ReviewGetResponse getReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId).orElseThrow(
                () -> new ServiceErrorException(ReviewErrorEnum.REVIEW_NOT_FOUND));

        return new ReviewGetResponse(
                review.getId(),
                review.getAuctionId(),
                review.getReviewerId(),
                review.getRevieweeId(),
                review.getScore(),
                review.getDescription(),
                review.getCreatedAt(),
                review.getModifiedAt()
        );
    }

    @Transactional
    public ReviewModifyResponse modifyReview(Long userId, Long reviewId, ReviewModifyRequest request) {
        Review review = reviewRepository.findById(reviewId).orElseThrow(
                () -> new ServiceErrorException(ReviewErrorEnum.REVIEW_NOT_FOUND));

        if (!review.getReviewerId().equals(userId)) {
            throw new ServiceErrorException(ReviewErrorEnum.REVIEW_FORBIDDEN);
        }

        review.modify(request);

        if (request.score() != null) {
            userRepository.findById(review.getRevieweeId()).ifPresent(reviewee -> {
                if (!reviewee.isDeleted()) {
                    Double avgScore = reviewRepository.findAvgScoreByRevieweeId(reviewee.getId());
                    BigDecimal rating = BigDecimal.valueOf(avgScore).setScale(1, RoundingMode.HALF_UP);
                    reviewee.updateRating(rating);
                }
            });
        }

        return new ReviewModifyResponse(
                review.getId(),
                review.getScore(),
                review.getDescription(),
                review.getCreatedAt(),
                review.getModifiedAt()
        );
    }

    @Transactional
    public void deleteReview(Long userId, Long reviewId) {
        Review review = reviewRepository.findById(reviewId).orElseThrow(
                () -> new ServiceErrorException(ReviewErrorEnum.REVIEW_NOT_FOUND));

        if (!review.getReviewerId().equals(userId)) {
            throw new ServiceErrorException(ReviewErrorEnum.REVIEW_FORBIDDEN);
        }

        Long revieweeId = review.getRevieweeId();

        reviewRepository.delete(review);

        userRepository.findById(revieweeId).ifPresent(reviewee -> {
            if (!reviewee.isDeleted()) {
                Double avgScore = reviewRepository.findAvgScoreByRevieweeId(reviewee.getId());
                BigDecimal rating = avgScore != null ? BigDecimal.valueOf(avgScore).setScale(1, RoundingMode.HALF_UP) : null;
                reviewee.updateRating(rating);
            }
        });
    }
}
