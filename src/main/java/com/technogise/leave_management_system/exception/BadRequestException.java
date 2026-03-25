package com.technogise.leave_management_system.exception;

import lombok.Getter;

@Getter
public class BadRequestException extends RuntimeException {
    private final String code;
    public BadRequestException(String message) {
        super(message);
        this.code = "BAD_REQUEST";
    }
}
