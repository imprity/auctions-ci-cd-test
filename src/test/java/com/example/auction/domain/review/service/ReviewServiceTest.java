package com.example.auction.domain.review.service;

import com.example.auction.common.dto.PageResponse;
import com.example.auction.common.exception.ServiceErrorException;
import com.example.auction.domain.auction.result.entity.AuctionResult;
import com.example.auction.domain.auction.result.exception.AuctionResultErrorEnum;
import com.example.auction.domain.auction.result.repository.AuctionResultRepository;
import com.example.auction.domain.review.dto.*;
import com.example.auction.domain.review.entity.Review;
import com.example.auction.domain.review.exception.ReviewErrorEnum;
import com.example.auction.domain.review.repository.ReviewRepository;
import com.example.auction.domain.user.entity.User;
import com.example.auction.domain.user.enums.UserRole;
import com.example.auction.domain.user.exception.UserErrorEnum;
import com.example.auction.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @InjectMocks
    private ReviewService reviewService;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuctionResultRepository auctionResultRepository;

    // ========================
    // 리뷰 생성
    // ========================

    @Test
    @DisplayName("리뷰 생성 성공")
    void createReview_success() {
        // given
        Long buyerId = 1L;
        Long sellerId = 2L;
        Long auctionId = 10L;
        Long bidId = 10L;

        ReviewCreateRequest request = new ReviewCreateRequest(auctionId, 5, "좋아요");

        AuctionResult auctionResult = AuctionResult.of(BigDecimal.valueOf(1000), auctionId, buyerId, sellerId, bidId);
        ReflectionTestUtils.setField(auctionResult, "id", 1L);

        User reviewee = User.of("seller@test.com", "encodedPassword", UserRole.USER);
        ReflectionTestUtils.setField(reviewee, "id", sellerId);

        given(reviewRepository.existsByAuctionIdAndReviewerId(auctionId, buyerId)).willReturn(false);
        given(auctionResultRepository.findByAuctionId(auctionId)).willReturn(Optional.of(auctionResult));
        given(userRepository.findByIdAndDeletedFalse(sellerId)).willReturn(Optional.of(reviewee));
        given(reviewRepository.save(any(Review.class))).willAnswer(inv -> inv.getArgument(0));
        given(reviewRepository.findAvgScoreByRevieweeId(sellerId)).willReturn(5.0);

        // when
        ReviewCreateResponse response = reviewService.createReview(buyerId, request);

        // then
        assertThat(response.auctionId()).isEqualTo(auctionId);
        assertThat(response.reviewerId()).isEqualTo(buyerId);
        assertThat(response.revieweeId()).isEqualTo(sellerId);
        assertThat(response.score()).isEqualTo(5);
    }

    @Test
    @DisplayName("리뷰 생성 실패 - 이미 작성한 리뷰")
    void createReview_fail_alreadyReviewed() {
        // given
        Long userId = 1L;
        Long auctionId = 10L;
        ReviewCreateRequest request = new ReviewCreateRequest(auctionId, 5, "좋아요");

        given(reviewRepository.existsByAuctionIdAndReviewerId(auctionId, userId)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> reviewService.createReview(userId, request))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(ReviewErrorEnum.ALREADY_REVIEWED.getMessage());
    }

    @Test
    @DisplayName("리뷰 생성 실패 - 경매 결과 없음")
    void createReview_fail_auctionResultNotFound() {
        // given
        Long userId = 1L;
        Long auctionId = 10L;
        ReviewCreateRequest request = new ReviewCreateRequest(auctionId, 5, "좋아요");

        given(reviewRepository.existsByAuctionIdAndReviewerId(auctionId, userId)).willReturn(false);
        given(auctionResultRepository.findByAuctionId(auctionId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> reviewService.createReview(userId, request))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(AuctionResultErrorEnum.AUCTION_RESULT_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("리뷰 생성 실패 - 경매 구매자, 판매자 아님")
    void createReview_fail_notAllowed() {
        // given
        Long userId = 99L;
        Long buyerId = 1L;
        Long sellerId = 2L;
        Long auctionId = 10L;
        Long bidId = 10L;
        ReviewCreateRequest request = new ReviewCreateRequest(auctionId, 5, "좋아요");

        AuctionResult auctionResult = AuctionResult.of(BigDecimal.valueOf(1000), auctionId, buyerId, sellerId, bidId);

        given(reviewRepository.existsByAuctionIdAndReviewerId(auctionId, userId)).willReturn(false);
        given(auctionResultRepository.findByAuctionId(auctionId)).willReturn(Optional.of(auctionResult));

        // when & then
        assertThatThrownBy(() -> reviewService.createReview(userId, request))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(ReviewErrorEnum.REVIEW_NOT_ALLOWED.getMessage());
    }

    @Test
    @DisplayName("리뷰 생성 실패 - 리뷰 대상 유저 없음")
    void createReview_fail_revieweeNotFound() {
        // given
        Long buyerId = 1L;
        Long sellerId = 2L;
        Long auctionId = 10L;
        Long bidId = 10L;
        ReviewCreateRequest request = new ReviewCreateRequest(auctionId, 5, "좋아요");

        AuctionResult auctionResult = AuctionResult.of(BigDecimal.valueOf(1000), auctionId, buyerId, sellerId, bidId);

        given(reviewRepository.existsByAuctionIdAndReviewerId(auctionId, buyerId)).willReturn(false);
        given(auctionResultRepository.findByAuctionId(auctionId)).willReturn(Optional.of(auctionResult));
        given(userRepository.findByIdAndDeletedFalse(sellerId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> reviewService.createReview(buyerId, request))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(UserErrorEnum.USER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("리뷰 생성 실패 - 동시 중복 리뷰")
    void createReview_fail_dataIntegrityViolation() {
        // given
        Long buyerId = 1L;
        Long sellerId = 2L;
        Long auctionId = 10L;
        Long bidId = 10L;
        ReviewCreateRequest request = new ReviewCreateRequest(auctionId, 5, "좋아요");

        AuctionResult auctionResult = AuctionResult.of(BigDecimal.valueOf(1000), auctionId, buyerId, sellerId, bidId);

        User reviewee = User.of("seller@test.com", "encodedPassword", UserRole.USER);
        ReflectionTestUtils.setField(reviewee, "id", sellerId);

        given(reviewRepository.existsByAuctionIdAndReviewerId(auctionId, buyerId)).willReturn(false);
        given(auctionResultRepository.findByAuctionId(auctionId)).willReturn(Optional.of(auctionResult));
        given(userRepository.findByIdAndDeletedFalse(sellerId)).willReturn(Optional.of(reviewee));
        given(reviewRepository.save(any(Review.class))).willThrow(new DataIntegrityViolationException("동시 중복 리뷰"));

        // when & then
        assertThatThrownBy(() -> reviewService.createReview(buyerId, request))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(ReviewErrorEnum.ALREADY_REVIEWED.getMessage());
    }


    // ========================
    // 작성한 리뷰 목록 조회
    // ========================

    @Test
    @DisplayName("작성한 리뷰 목록 조회 성공")
    void getWrittenReviewList_success() {
        // given
        Long userId = 1L;
        ReviewSearchCondition condition = new ReviewSearchCondition();

        List<ReviewListGetResponse> reviewList = List.of(
                new ReviewListGetResponse(1L, 10L, 2L, LocalDateTime.now(), LocalDateTime.now()),
                new ReviewListGetResponse(2L, 11L, 3L, LocalDateTime.now(), LocalDateTime.now()),
                new ReviewListGetResponse(3L, 12L, 4L, LocalDateTime.now(), LocalDateTime.now()),
                new ReviewListGetResponse(4L, 13L, 5L, LocalDateTime.now(), LocalDateTime.now()),
                new ReviewListGetResponse(5L, 14L, 6L, LocalDateTime.now(), LocalDateTime.now()),
                new ReviewListGetResponse(6L, 15L, 7L, LocalDateTime.now(), LocalDateTime.now()),
                new ReviewListGetResponse(7L, 16L, 8L, LocalDateTime.now(), LocalDateTime.now()),
                new ReviewListGetResponse(8L, 17L, 9L, LocalDateTime.now(), LocalDateTime.now()),
                new ReviewListGetResponse(9L, 18L, 10L, LocalDateTime.now(), LocalDateTime.now()),
                new ReviewListGetResponse(10L, 19L, 11L, LocalDateTime.now(), LocalDateTime.now()),
                new ReviewListGetResponse(11L, 20L, 12L, LocalDateTime.now(), LocalDateTime.now())
        );
        Page<ReviewListGetResponse> page = new PageImpl<>(reviewList.subList(0, 10), PageRequest.of(0, 10), 11);

        given(reviewRepository.findWrittenReviewsWithConditions(eq(userId), any(Pageable.class), any(), any())).willReturn(page);

        // when
        PageResponse<ReviewListGetResponse> response = reviewService.getWrittenReviewList(userId, condition);

        // then
        assertThat(response.content()).hasSize(10);
        assertThat(response.totalPages()).isEqualTo(2);
        assertThat(response.totalElements()).isEqualTo(11);
        assertThat(response.currentPage()).isEqualTo(0);
        assertThat(response.size()).isEqualTo(10);
        assertThat(response.isLast()).isFalse();
    }

    @Test
    @DisplayName("작성한 리뷰 목록 조회 성공 - 작성한 리뷰 없음")
    void getWrittenReviewList_success_empty() {
        // given
        Long userId = 1L;
        ReviewSearchCondition condition = new ReviewSearchCondition();

        Page<ReviewListGetResponse> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

        given(reviewRepository.findWrittenReviewsWithConditions(eq(userId), any(Pageable.class), any(), any())).willReturn(page);

        // when
        PageResponse<ReviewListGetResponse> response = reviewService.getWrittenReviewList(userId, condition);

        // then
        assertThat(response.content()).isEmpty();
        assertThat(response.totalPages()).isEqualTo(0);
        assertThat(response.totalElements()).isEqualTo(0);
        assertThat(response.currentPage()).isEqualTo(0);
        assertThat(response.size()).isEqualTo(10);
        assertThat(response.isLast()).isTrue();
    }


    // ========================
    // 받은 리뷰 목록 조회
    // ========================

    @Test
    @DisplayName("받은 리뷰 목록 조회 성공")
    void getReceivedReviewList_success() {
        // given
        Long userId = 1L;
        ReviewSearchCondition condition = new ReviewSearchCondition();

        List<ReviewListGetResponse> reviewList = List.of(
                new ReviewListGetResponse(1L, 10L, 2L, LocalDateTime.now(), LocalDateTime.now()),
                new ReviewListGetResponse(2L, 11L, 3L, LocalDateTime.now(), LocalDateTime.now()),
                new ReviewListGetResponse(3L, 12L, 4L, LocalDateTime.now(), LocalDateTime.now()),
                new ReviewListGetResponse(4L, 13L, 5L, LocalDateTime.now(), LocalDateTime.now()),
                new ReviewListGetResponse(5L, 14L, 6L, LocalDateTime.now(), LocalDateTime.now()),
                new ReviewListGetResponse(6L, 15L, 7L, LocalDateTime.now(), LocalDateTime.now()),
                new ReviewListGetResponse(7L, 16L, 8L, LocalDateTime.now(), LocalDateTime.now()),
                new ReviewListGetResponse(8L, 17L, 9L, LocalDateTime.now(), LocalDateTime.now()),
                new ReviewListGetResponse(9L, 18L, 10L, LocalDateTime.now(), LocalDateTime.now()),
                new ReviewListGetResponse(10L, 19L, 11L, LocalDateTime.now(), LocalDateTime.now()),
                new ReviewListGetResponse(11L, 20L, 12L, LocalDateTime.now(), LocalDateTime.now())
        );
        Page<ReviewListGetResponse> page = new PageImpl<>(reviewList.subList(0, 10), PageRequest.of(0, 10), 11);

        given(reviewRepository.findReceivedReviewsWithConditions(eq(userId), any(Pageable.class), any(), any())).willReturn(page);

        // when
        PageResponse<ReviewListGetResponse> response = reviewService.getReceivedReviewList(userId, condition);

        // then
        assertThat(response.content()).hasSize(10);
        assertThat(response.totalPages()).isEqualTo(2);
        assertThat(response.totalElements()).isEqualTo(11);
        assertThat(response.currentPage()).isEqualTo(0);
        assertThat(response.size()).isEqualTo(10);
        assertThat(response.isLast()).isFalse();
    }

    @Test
    @DisplayName("받은 리뷰 목록 조회 성공 - 받은 리뷰 없음")
    void getReceivedReviewList_success_empty() {
        // given
        Long userId = 1L;
        ReviewSearchCondition condition = new ReviewSearchCondition();

        Page<ReviewListGetResponse> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

        given(reviewRepository.findReceivedReviewsWithConditions(eq(userId), any(Pageable.class), any(), any())).willReturn(page);

        // when
        PageResponse<ReviewListGetResponse> response = reviewService.getReceivedReviewList(userId, condition);

        // then
        assertThat(response.content()).isEmpty();
        assertThat(response.totalPages()).isEqualTo(0);
        assertThat(response.totalElements()).isEqualTo(0);
        assertThat(response.currentPage()).isEqualTo(0);
        assertThat(response.size()).isEqualTo(10);
        assertThat(response.isLast()).isTrue();
    }


    // ========================
    // 리뷰 상세 조회
    // ========================

    @Test
    @DisplayName("리뷰 상세 조회 성공")
    void getReview_success() {
        // given
        Long reviewId = 1L;

        Review review = Review.of(10L, 1L, 2L, 5, "좋아요");
        ReflectionTestUtils.setField(review, "id", reviewId);

        given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));

        // when
        ReviewGetResponse response = reviewService.getReview(reviewId);

        // then
        assertThat(response.reviewId()).isEqualTo(reviewId);
        assertThat(response.auctionId()).isEqualTo(10L);
        assertThat(response.reviewerId()).isEqualTo(1L);
        assertThat(response.revieweeId()).isEqualTo(2L);
        assertThat(response.score()).isEqualTo(5);
        assertThat(response.description()).isEqualTo("좋아요");
    }

    @Test
    @DisplayName("리뷰 상세 조회 실패 - 리뷰 없음")
    void getReview_fail_reviewNotFound() {
        // given
        Long reviewId = 1L;

        given(reviewRepository.findById(reviewId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> reviewService.getReview(reviewId))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(ReviewErrorEnum.REVIEW_NOT_FOUND.getMessage());
    }


    // ========================
    // 리뷰 수정
    // ========================

    @Test
    @DisplayName("리뷰 수정 성공 - score 수정")
    void modifyReview_success() {
        // given
        Long userId = 1L;
        Long reviewId = 1L;
        ReviewModifyRequest request = new ReviewModifyRequest(1, "별로에요");

        Review review = Review.of(10L, userId, 2L, 5, "좋아요");
        ReflectionTestUtils.setField(review, "id", reviewId);

        User reviewee = User.of("seller@test.com", "encodedPassword", UserRole.USER);
        ReflectionTestUtils.setField(reviewee, "id", 2L);

        given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));
        given(userRepository.findById(reviewee.getId())).willReturn(Optional.of(reviewee));
        given(reviewRepository.findAvgScoreByRevieweeId(reviewee.getId())).willReturn(1.0);

        // when
        ReviewModifyResponse response = reviewService.modifyReview(userId, reviewId, request);

        // then
        assertThat(response.score()).isEqualTo(1);
        assertThat(response.description()).isEqualTo("별로에요");
    }

    @Test
    @DisplayName("리뷰 수정 성공 - score 수정 X")
    void modifyReview_success_descriptionOnly() {
        // given
        Long userId = 1L;
        Long reviewId = 1L;
        ReviewModifyRequest request = new ReviewModifyRequest(null, "친절해요");

        Review review = Review.of(10L, userId, 2L, 5, "좋아요");
        ReflectionTestUtils.setField(review, "id", reviewId);

        given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));

        // when
        ReviewModifyResponse response = reviewService.modifyReview(userId, reviewId, request);

        // then
        assertThat(response.score()).isEqualTo(5);
        assertThat(response.description()).isEqualTo("친절해요");
    }

    @Test
    @DisplayName("리뷰 수정 실패 - 리뷰 없음")
    void modifyReview_fail_reviewNotFound() {
        // given
        Long userId = 1L;
        Long reviewId = 1L;
        ReviewModifyRequest request = new ReviewModifyRequest(1, "별로에요");

        given(reviewRepository.findById(reviewId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> reviewService.modifyReview(userId, reviewId, request))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(ReviewErrorEnum.REVIEW_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("리뷰 수정 실패 - 리뷰 작성자 아님")
    void modifyReview_fail_forbidden() {
        // given
        Long userId = 99L;
        Long reviewId = 1L;
        ReviewModifyRequest request = new ReviewModifyRequest(1, "별로에요");

        Review review = Review.of(10L, 1L, 2L, 5, "좋아요");
        ReflectionTestUtils.setField(review, "id", reviewId);

        given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));

        // when & then
        assertThatThrownBy(() -> reviewService.modifyReview(userId, reviewId, request))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(ReviewErrorEnum.REVIEW_FORBIDDEN.getMessage());
    }

    @Test
    @DisplayName("리뷰 수정 실패 - 수정할 내용 없음")
    void modifyReview_fail_noContent() {
        // given
        Long userId = 1L;
        Long reviewId = 1L;
        ReviewModifyRequest request = new ReviewModifyRequest(null, null);

        Review review = Review.of(10L, userId, 2L, 5, "좋아요");
        ReflectionTestUtils.setField(review, "id", reviewId);

        given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));

        // when & then
        assertThatThrownBy(() -> reviewService.modifyReview(userId, reviewId, request))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(ReviewErrorEnum.REVIEW_MODIFY_NO_CONTENT.getMessage());
    }


    // ========================
    // 리뷰 삭제
    // ========================

    @Test
    @DisplayName("리뷰 삭제 성공")
    void deleteReview_success() {
        // given
        Long userId = 1L;
        Long reviewId = 1L;

        Review review = Review.of(10L, userId, 2L, 5, "좋아요");
        ReflectionTestUtils.setField(review, "id", reviewId);

        User reviewee = User.of("seller@test.com", "encodedPassword", UserRole.USER);
        ReflectionTestUtils.setField(reviewee, "id", 2L);
        ReflectionTestUtils.setField(reviewee, "rating", BigDecimal.valueOf(3.0));

        given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));
        given(userRepository.findById(2L)).willReturn(Optional.of(reviewee));
        given(reviewRepository.findAvgScoreByRevieweeId(2L)).willReturn(1.0);

        // when
        reviewService.deleteReview(userId, reviewId);

        // then
        verify(reviewRepository).delete(review);
        assertThat(reviewee.getRating()).isEqualByComparingTo(BigDecimal.valueOf(1.0));
    }

    @Test
    @DisplayName("리뷰 삭제 성공 - 마지막 리뷰 삭제 시 null")
    void deleteReview_success_lastReview() {
        // given
        Long userId = 1L;
        Long reviewId = 1L;

        Review review = Review.of(10L, userId, 2L, 5, "좋아요");
        ReflectionTestUtils.setField(review, "id", reviewId);

        User reviewee = User.of("seller@test.com", "encodedPassword", UserRole.USER);
        ReflectionTestUtils.setField(reviewee, "id", 2L);
        ReflectionTestUtils.setField(reviewee, "rating", BigDecimal.valueOf(5.0));

        given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));
        given(userRepository.findById(2L)).willReturn(Optional.of(reviewee));
        given(reviewRepository.findAvgScoreByRevieweeId(2L)).willReturn(null);

        // when
        reviewService.deleteReview(userId, reviewId);

        // then
        verify(reviewRepository).delete(review);
        assertThat(reviewee.getRating()).isNull();
    }

    @Test
    @DisplayName("리뷰 삭제 성공 - 리뷰 대상자 탈퇴 시 평점 업데이트 스킵")
    void deleteReview_success_revieweeDeleted() {
        // given
        Long userId = 1L;
        Long reviewId = 1L;

        Review review = Review.of(10L, userId, 2L, 5, "좋아요");
        ReflectionTestUtils.setField(review, "id", reviewId);

        User reviewee = User.of("seller@test.com", "encodedPassword", UserRole.USER);
        ReflectionTestUtils.setField(reviewee, "id", 2L);
        ReflectionTestUtils.setField(reviewee, "deleted", true);

        given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));
        given(userRepository.findById(2L)).willReturn(Optional.of(reviewee));

        // when
        reviewService.deleteReview(userId, reviewId);

        // then
        verify(reviewRepository).delete(review);
        verify(reviewRepository, never()).findAvgScoreByRevieweeId(any());
    }

    @Test
    @DisplayName("리뷰 삭제 실패 - 리뷰 없음")
    void deleteReview_fail_reviewNotFound() {
        // given
        Long userId = 1L;
        Long reviewId = 1L;

        given(reviewRepository.findById(reviewId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> reviewService.deleteReview(userId, reviewId))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(ReviewErrorEnum.REVIEW_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("리뷰 삭제 실패 - 리뷰 작성자 아님")
    void deleteReview_fail_forbidden() {
        // given
        Long userId = 99L;
        Long reviewId = 1L;

        Review review = Review.of(10L, 1L, 2L, 5, "좋아요");
        ReflectionTestUtils.setField(review, "id", reviewId);

        given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));

        // when & then
        assertThatThrownBy(() -> reviewService.deleteReview(userId, reviewId))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(ReviewErrorEnum.REVIEW_FORBIDDEN.getMessage());
    }
}