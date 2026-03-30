package com.technogise.leave_management_system.exception;


import com.technogise.leave_management_system.response.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldReturnNotFoundErrorResponseWhenNotFoundHttpExceptionThrown() {
        HttpException exception = new HttpException(
    void shouldReturnNotFoundErrorResponseWhenNotFoundApplicationExceptionThrown() {
        ApplicationException exception = new ApplicationException(
                HttpStatus.NOT_FOUND,
                "User not found"
        );
        ResponseEntity<ErrorResponse> response =
                handler.handleApplicationException(exception);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("User not found", response.getBody().getMessage());
    void shouldReturnBadRequestStatusWithBodyWhenBadRequestExceptionIsThrown() {
        BadRequestException exception = new BadRequestException("Invalid input");
        ResponseEntity<String> response = globalExceptionHandler.handleBadRequestException(exception);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid input", response.getBody());
    void shouldReturnNotFoundErrorResponseWhenNotFoundApplicationThrownException() {
        ApplicationException exception = new ApplicationException(
                HttpStatus.NOT_FOUND,
                "User not found"
        );
        ResponseEntity<ErrorResponse> response =
                handler.handleApplicationException(exception);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("User not found", response.getBody().getMessage());
        assertEquals("Not Found", response.getBody().getCode());
    }

    @Test
    void shouldReturnBadRequestWhenHttpMessageNotReadableExceptionThrown() {
        HttpMessageNotReadableException ex = mock(HttpMessageNotReadableException.class);
        Throwable cause = new RuntimeException("Malformed JSON request");
        when(ex.getMostSpecificCause()).thenReturn(cause);

        ResponseEntity<ErrorResponse> response = handler.handleHttpMessageNotReadable(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("400",         response.getBody().getStatus());
        assertEquals("Bad Request", response.getBody().getCode());
        assertEquals("Malformed JSON request", response.getBody().getMessage());
    }

}
