package com.technogise.leave_management_system.exception;


import com.technogise.leave_management_system.response.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldReturnErrorResponseWhenApplicationThrownException() {
        ApplicationException exception = new ApplicationException(
                HttpStatus.NOT_FOUND,
                "User not found"
        );
        ResponseEntity<ErrorResponse> response =
                handler.handleApplicationException(exception);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("User not found", response.getBody().getMessage());
    }
}
