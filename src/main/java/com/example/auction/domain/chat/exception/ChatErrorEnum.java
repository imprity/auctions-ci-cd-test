package com.example.auction.domain.chat.exception;

import com.example.auction.common.exception.ErrorEnumInterface;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ChatErrorEnum implements ErrorEnumInterface {

    // 채팅방
    CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "채팅방을 찾을 수 없습니다"),
    CHAT_ROOM_FORBIDDEN(HttpStatus.FORBIDDEN, "본인 채팅방만 이용할 수 있습니다"),

    // 메시지
    CHAT_MESSAGE_EMPTY(HttpStatus.BAD_REQUEST, "메시지 내용을 입력해 주세요");

    private final HttpStatus status;
    private final String message;

    ChatErrorEnum(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}