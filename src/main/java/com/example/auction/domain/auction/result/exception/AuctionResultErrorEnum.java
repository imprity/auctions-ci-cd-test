package com.example.auction.domain.auction.result.exception;

import com.example.auction.common.exception.ErrorEnumInterface;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum AuctionResultErrorEnum implements ErrorEnumInterface {

    // 경매 결과 관련
    AUCTION_RESULT_NOT_FOUND(HttpStatus.NOT_FOUND, "경매 결과를 찾을 수 없습니다");

    private final HttpStatus status;
    private final String message;

    AuctionResultErrorEnum(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}