package com.example.auction.domain.review.repository;

import com.example.auction.common.config.JpaConfig;
import com.example.auction.common.config.QuerydslConfig;
import com.example.auction.domain.review.dto.ReviewListGetResponse;
import com.example.auction.domain.review.entity.Review;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace= AutoConfigureTestDatabase.Replace.NONE)
@Import({QuerydslConfig.class, JpaConfig.class})
class ReviewCustomRepositoryImplTest {

    @Autowired
    private ReviewRepository reviewRepository;

    @BeforeEach
    void setUp() {
        reviewRepository.save(Review.of(10L, 1L, 2L, 5, "좋아요"));
        reviewRepository.save(Review.of(11L, 1L, 3L, 4, "괜찮아요"));
        reviewRepository.save(Review.of(12L, 3L, 2L, 3, "보통이에요"));
    }

    // ========================
    // 작성한 리뷰 목록 조회
    // ========================

    @Test
    @DisplayName("작성한 리뷰 목록 조회 - 전체 조회")
    void findWrittenReviewsWithConditions_noFilter() {
        Page<ReviewListGetResponse> result = reviewRepository.findWrittenReviewsWithConditions(
                1L, PageRequest.of(0, 10), null, null
        );

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("작성한 리뷰 목록 조회 - 없으면 빈 결과")
    void findWrittenReviewsWithConditions_otherUser() {
        Page<ReviewListGetResponse> result = reviewRepository.findWrittenReviewsWithConditions(
                99L, PageRequest.of(0, 10), null, null
        );

        assertThat(result.getTotalElements()).isEqualTo(0);
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("작성한 리뷰 목록 조회 - 날짜 범위 필터링")
    void findWrittenReviewsWithConditions_dateFilter() {
        Page<ReviewListGetResponse> inRange = reviewRepository.findWrittenReviewsWithConditions(
                1L, PageRequest.of(0, 10),
                LocalDate.now().minusDays(1),
                LocalDate.now().plusDays(1)
        );

        Page<ReviewListGetResponse> outOfRange = reviewRepository.findWrittenReviewsWithConditions(
                1L, PageRequest.of(0, 10),
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(2)
        );

        assertThat(inRange.getContent()).hasSize(2);
        assertThat(outOfRange.getContent()).isEmpty();
    }


    // ========================
    // 받은 리뷰 목록 조회
    // ========================

    @Test
    @DisplayName("받은 리뷰 목록 조회 - 전체 조회")
    void findReceivedReviewsWithConditions_noFilter() {
        Page<ReviewListGetResponse> result = reviewRepository.findReceivedReviewsWithConditions(
                2L, PageRequest.of(0, 10), null, null
        );

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("받은 리뷰 목록 조회 - 없으면 빈 결과")
    void findReceivedReviewsWithConditions_otherUser() {
        Page<ReviewListGetResponse> result = reviewRepository.findReceivedReviewsWithConditions(
                99L, PageRequest.of(0, 10), null, null
        );

        assertThat(result.getTotalElements()).isEqualTo(0);
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("받은 리뷰 목록 조회 - 날짜 범위 필터링")
    void findReceivedReviewsWithConditions_dateFilter() {
        Page<ReviewListGetResponse> inRange = reviewRepository.findReceivedReviewsWithConditions(
                2L, PageRequest.of(0, 10),
                LocalDate.now().minusDays(1),
                LocalDate.now().plusDays(1)
        );

        Page<ReviewListGetResponse> outOfRange = reviewRepository.findReceivedReviewsWithConditions(
                2L, PageRequest.of(0, 10),
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(2)
        );

        assertThat(inRange.getContent()).hasSize(2);
        assertThat(outOfRange.getContent()).isEmpty();
    }
}