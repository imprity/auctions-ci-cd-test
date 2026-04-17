package com.example.auction.domain.review.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class ReviewSearchCondition {

    @PositiveOrZero(message = "페이지는 0 이상이어야 합니다")
    private int page = 0;

    @Positive(message = "페이지 크기는 1 이상이어야 합니다")
    private int size = 10;

    private LocalDate startDate;
    private LocalDate endDate;

    @AssertTrue(message = "시작일은 종료일보다 이후일 수 없습니다")
    private boolean isValidDateRange() {
        return startDate == null || endDate == null || !startDate.isAfter(endDate);
    }
}
