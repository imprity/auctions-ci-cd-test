package com.example.auction.domain.auction.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.auction.domain.auction.enums.AuctionProductCategory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CreateAuctionRequest {
    @Size(max=256, message = "경매 상품 이름이 너무 깁니다")
    @NotBlank(message = "경매 상품 이름은 필수입니다")
    private String itemName;

    @Size(max=1024, message = "경매 설명이 너무 깁니다")
    private String description;

    @Positive(message = "경매 상품의 가격은 0보다 커야합니다")
    @NotNull
    private BigDecimal maxPrice;

    @NotNull
    private AuctionProductCategory category;

    @NotNull
    private LocalDateTime startedAt;
    @NotNull
    private LocalDateTime endedAt;
}
