package com.example.auction.domain.auction.exception;

import com.example.auction.common.exception.ErrorEnumInterface;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum AuctionErrorEnum implements ErrorEnumInterface {

    // 경매 관련 에러
    AUCTION_NOT_FOUND(
            HttpStatus.NOT_FOUND, 
            "경매를 찾을 수 없습니다"
    ),

    AUCTION_STATUS_NOT_CANCELLABLE(
            HttpStatus.CONFLICT,
            "경매를 취소할 수 있는 상태가 아닙니다"
    ),
    AUCTION_FORBIDDEN_FROM_CANCEL(
            HttpStatus.FORBIDDEN,
            "경매를 취소할 수 있는 권한이 없습니다"
    ),
    AUCTION_TOO_LATE_TO_CANCEL(
            HttpStatus.CONFLICT,
            "경매를 취소하기에 너무 늦었습니다"
    ),


    AUCTION_SEARCH_FORBIDDEN_STATUS_FILTER(
            HttpStatus.BAD_REQUEST, 
            "취소된 경매는 전체조회에서 볼 수 없습니다"
    ),
    AUCTION_SEARCH_INVLID_PRICE_RANGE(
            HttpStatus.BAD_REQUEST, 
            "조회 최소 금액이 최대 금액보다 클 수는 없습니다"
    ),

    AUCTION_CREATE_STARTED_AT_IN_PAST(
            HttpStatus.BAD_REQUEST,
            "경매 시작 시간은 현재 시간 이후여야 합니다"
    ),
    AUCTION_CREATE_ENDED_AT_IN_PAST(
            HttpStatus.BAD_REQUEST,
            "경매 종료 시간은 현재 시간 이후여야 합니다"
    ),
    AUCTION_CREATE_ENDED_AT_BEFORE_STARTED_AT(
            HttpStatus.BAD_REQUEST,
            "경매 종료 시간은 경매 시작 시간 이후여야 합니다"
    ),
    AUCTION_INVALID_STATUS(
            HttpStatus.CONFLICT,
            "경매 상태가 유효하지 않습니다"
    );

    private final HttpStatus status;
    private final String message;

    AuctionErrorEnum(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
