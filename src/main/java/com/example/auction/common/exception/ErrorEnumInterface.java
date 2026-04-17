package com.example.auction.common.exception;

import org.springframework.http.HttpStatus;

public interface ErrorEnumInterface {
    HttpStatus getStatus();
    String getMessage();
}
