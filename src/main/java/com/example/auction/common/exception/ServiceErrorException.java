package com.example.auction.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ServiceErrorException extends RuntimeException {
    private final HttpStatus httpStatus;

    public ServiceErrorException(ErrorEnumInterface errorEnum) {
        super(errorEnum.getMessage());
        this.httpStatus = errorEnum.getStatus();
    }
}
