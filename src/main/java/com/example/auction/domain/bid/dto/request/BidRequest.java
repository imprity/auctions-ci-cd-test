package com.example.auction.domain.bid.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;


@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BidRequest {

    @NotNull(message = "입찰 금액은 필수입니다")
    @Positive(message = "입찰 금액은 0보다 커야 합니다")
    private BigDecimal price;

    @Size(max = 1024, message = "설명이 너무 깁니다")
    private String description;

}
