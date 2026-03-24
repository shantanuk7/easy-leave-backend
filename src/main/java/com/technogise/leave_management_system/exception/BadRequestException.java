package com.technogise.leave_management_system.exception;

import lombok.Getter;

@Getter
public class InvalidQueryParameterException extends RuntimeException {
    private final String code;
    public InvalidQueryParameterException(String code, String message) {
        super(message);
        this.code = code;
    }
}
