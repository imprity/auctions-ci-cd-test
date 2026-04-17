package com.example.auction.domain.review.repository;

import com.example.auction.domain.review.dto.ReviewListGetResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface ReviewCustomRepository {
    Page<ReviewListGetResponse> findWrittenReviewsWithConditions(Long userId, Pageable pageable, LocalDate startDate, LocalDate endDate);

    Page<ReviewListGetResponse> findReceivedReviewsWithConditions(Long userId, Pageable pageable, LocalDate startDate, LocalDate endDate);
}
