package com.technogise.leave_management_system.exception;

import lombok.Getter;

@Getter
public class NotFoundException extends RuntimeException {

    private final String code;

    public NotFoundException( String message) {
        super(message);
        this.code = "NOT_FOUND";
    }
}
