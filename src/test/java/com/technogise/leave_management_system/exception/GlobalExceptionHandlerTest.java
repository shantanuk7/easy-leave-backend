package com.technogise.leave_management_system.exception;

import com.technogise.leave_management_system.response.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldReturnNotFoundErrorResponseWhenNotFoundHttpExceptionThrown() {
        HttpException exception = new HttpException(
                HttpStatus.NOT_FOUND,
                "User not found"
        );

        ResponseEntity<ErrorResponse> response = handler.handleApplicationException(exception);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("404",       response.getBody().getStatusCode());
        assertEquals("Not Found", response.getBody().getCode());
        assertEquals("User not found", response.getBody().getMessage());
    }

    @Test
    void shouldReturnBadRequestWhenHttpMessageNotReadableExceptionThrown() {
        HttpMessageNotReadableException ex = mock(HttpMessageNotReadableException.class);
        Throwable cause = new RuntimeException("Malformed JSON request");
        when(ex.getMostSpecificCause()).thenReturn(cause);

        ResponseEntity<ErrorResponse> response = handler.handleHttpMessageNotReadable(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("400",         response.getBody().getStatusCode());
        assertEquals("Bad Request", response.getBody().getCode());
        assertEquals("Malformed JSON request", response.getBody().getMessage());
    }

    @Test
    void shouldReturnBadRequestWhenMethodArgumentNotValidExceptionThrown() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError(
                "createLeaveRequest",
                "dates",
                "At least one date must be provided"
        );

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        ResponseEntity<ErrorResponse> response = handler.handleMethodArgumentNotValidException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("400",         response.getBody().getStatusCode());
        assertEquals("Bad Request", response.getBody().getCode());
        assertEquals("At least one date must be provided", response.getBody().getMessage());
    }

    @Test
    void shouldReturnInternalServerErrorWhenExceptionThrown() {
        String errorMessage = "Something went wrong internally";
        Exception ex = new RuntimeException(errorMessage);

        ResponseEntity<ErrorResponse> response = handler.handleException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("500",                   response.getBody().getStatusCode());
        assertEquals("Internal Server Error", response.getBody().getCode());
        assertEquals(errorMessage,            response.getBody().getMessage());
    }
}
