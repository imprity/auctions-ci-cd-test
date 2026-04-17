package com.example.auction.domain.auction.dto;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import com.example.auction.domain.auction.enums.AuctionProductCategory;
import com.example.auction.domain.auction.enums.AuctionStatus;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AuctionSearchCondition {
    private @Nullable String keyword;

    @PositiveOrZero(message = "최소 금액은 0 이상이어야 합니다")
    private @Nullable BigDecimal maxPriceMin;
    @Positive(message = "최대 금액은 0보다 커야합니다")
    private @Nullable BigDecimal maxPriceMax;

    private @Nullable Set<AuctionStatus> status;

    private @Nullable AuctionProductCategory category;

    @PositiveOrZero(message = "페이지 0 이상이어야 커야합니다")
    private Integer page = 0;

    @Positive(message = "페이지 크기는 0보다 커야합니다")
    private Integer pageSize = 10;

    public void setDefaultStatusesIfEmpty(AuctionStatus... statuses) {
        if (this.status == null) {
            this.status = new HashSet<>();
        }

        if (this.status.isEmpty()) {
            this.status.addAll(Arrays.asList(statuses));
        }
    }
}
