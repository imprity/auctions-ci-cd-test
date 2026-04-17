package com.example.auction.domain.auction.util;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

import com.example.auction.common.exception.ServiceErrorException;
import com.example.auction.domain.auction.dto.AuctionSearchCondition;
import com.example.auction.domain.auction.dto.CreateAuctionRequest;
import com.example.auction.domain.auction.exception.AuctionErrorEnum;

public class AuctionUtil {
    private AuctionUtil() {}

    public static void throwIfSearchConditionNotValid(
            AuctionSearchCondition condition
    ) {
        // 검색 조건중 최소금액이 최대 금액 보다 클경우 에러를 던지기
        if (
                condition.getMaxPriceMin() != null &&
                condition.getMaxPriceMax() != null &&
                condition.getMaxPriceMax().compareTo(condition.getMaxPriceMin()) < 0
        ) {
            throw new ServiceErrorException(AuctionErrorEnum.AUCTION_SEARCH_INVLID_PRICE_RANGE);
        }
    }

    public static void throwIfCreateAuctionRequestNotValid(
            CreateAuctionRequest req
    ) {
        LocalDateTime now = LocalDateTime.now();

        // 경매 시작 시간이 현재 시간 보다 과거이면 에러를 던지기
        if (req.getStartedAt().isBefore(now)) {
            throw new ServiceErrorException(AuctionErrorEnum.AUCTION_CREATE_STARTED_AT_IN_PAST);
        }

        // 경매 종료 시간이 현재 시간 보다 과거이면 에러를 던지기
        if (req.getEndedAt().isBefore(now)) {
            throw new ServiceErrorException(AuctionErrorEnum.AUCTION_CREATE_ENDED_AT_IN_PAST);
        }

        // 경매 종료 시간이 시작 시간 보다 과거이면 에러를 던지기
        if (req.getEndedAt().isBefore(req.getStartedAt())) {
            throw new ServiceErrorException(AuctionErrorEnum.AUCTION_CREATE_ENDED_AT_BEFORE_STARTED_AT);
        }
    }
}
