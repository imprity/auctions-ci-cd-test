package com.example.auction.domain.user.exception;

import com.example.auction.common.exception.ErrorEnumInterface;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum UserErrorEnum implements ErrorEnumInterface {

    // 유저 관련
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "유저를 찾을 수 없습니다"),
    HAS_ACTIVE_AUCTION(HttpStatus.CONFLICT, "진행 중인 경매가 있어 탈퇴할 수 없습니다"),
    HAS_ACTIVE_BID(HttpStatus.CONFLICT, "진행 중인 입찰이 있어 탈퇴할 수 없습니다");

    private final HttpStatus status;
    private final String message;

    UserErrorEnum(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}