package com.example.auction.domain.auth.exception;

import com.example.auction.common.exception.ErrorEnumInterface;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum AuthErrorEnum implements ErrorEnumInterface {

    // 인증/인가 관련
    DUPLICATED_EMAIL(HttpStatus.CONFLICT, "이미 사용중인 이메일입니다"),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "비밀번호가 일치하지 않습니다"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다"),
    SAME_AS_OLD_PASSWORD(HttpStatus.BAD_REQUEST, "기존 비밀번호와 동일한 비밀번호로 변경할 수 없습니다"),
    SOCIAL_LOGIN_EMAIL_CONFLICT(HttpStatus.CONFLICT, "이미 일반 회원가입으로 가입된 이메일입니다");

    private final HttpStatus status;
    private final String message;

    AuthErrorEnum(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}