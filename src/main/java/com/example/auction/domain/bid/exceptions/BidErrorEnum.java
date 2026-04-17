package com.example.auction.domain.bid.exceptions;

import com.example.auction.common.exception.ErrorEnumInterface;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum BidErrorEnum implements ErrorEnumInterface {

    // 입찰 관련 에러
    BID_NOT_FOUND(HttpStatus.NOT_FOUND, "입찰을 찾을 수 없습니다"),
    AUCTION_RESULT_NOT_FOUND(HttpStatus.NOT_FOUND, "경매 결과를 찾을 수 없습니다"),
    BID_FORBIDDEN_SELF_BID(HttpStatus.FORBIDDEN, "자신의 경매에는 입찰할 수 없습니다"),
    BID_PRICE_EXCEEDS_MAX(HttpStatus.BAD_REQUEST, "최대 가격을 초과하였으니 더 낮은 금액으로 입찰해주세요"),
    BID_PRICE_NOT_LOWER(HttpStatus.BAD_REQUEST, "현재 최저가보다 적은 금액으로 입찰해주세요"),
    BID_LOCK_FAILED(HttpStatus.CONFLICT, "입찰에 실패했습니다");

    private final HttpStatus status;
    private final String message;

    BidErrorEnum(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}