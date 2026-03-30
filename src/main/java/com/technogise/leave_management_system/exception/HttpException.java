package com.technogise.leave_management_system.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class HttpException extends RuntimeException {

    private final HttpStatus statusCode;

    public HttpException(HttpStatus statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }
}
