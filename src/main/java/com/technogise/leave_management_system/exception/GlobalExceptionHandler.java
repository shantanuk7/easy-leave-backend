package com.technogise.leave_management_system.exception;

import com.technogise.leave_management_system.response.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).
                body(new ErrorResponse("404",exception.getCode(), exception.getMessage()));
    }
    @ExceptionHandler(InvalidQueryParameterException.class)
    public ResponseEntity<ErrorResponse> handleInvalidQueryParameter(InvalidQueryParameterException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).
                body(new ErrorResponse("400",exception.getCode(), exception.getMessage()));
    }
}
