package com.technogise.leave_management_system.exception;
import lombok.Getter;

@Getter
public class ResourceNotFoundException extends RuntimeException {
    private final String code;

    public ResourceNotFoundException(String code, String message) {
        super(message);
        this.code = code;
    }
}
