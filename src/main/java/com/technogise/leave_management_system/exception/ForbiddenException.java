package com.technogise.leave_management_system.exception;

import lombok.Getter;

@Getter
public class ForbiddenException extends RuntimeException {
    private final String code;
    public ForbiddenException(String message) {
        super(message);
        this.code = "FORBIDDEN";
    }
}
