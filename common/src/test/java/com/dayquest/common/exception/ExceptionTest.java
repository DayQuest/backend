package com.dayquest.common.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Exception Tests")
class ExceptionTest {

    @Test
    @DisplayName("InvalidRequestException should store message")
    void invalidRequestExceptionShouldStoreMessage() {
        InvalidRequestException ex = new InvalidRequestException("Invalid input");

        assertEquals("Invalid input", ex.getMessage());
    }

    @Test
    @DisplayName("InvalidRequestException should store message and cause")
    void invalidRequestExceptionShouldStoreMessageAndCause() {
        Throwable cause = new RuntimeException("Root cause");
        InvalidRequestException ex = new InvalidRequestException("Invalid input", cause);

        assertEquals("Invalid input", ex.getMessage());
        assertEquals(cause, ex.getCause());
    }

    @Test
    @DisplayName("UserAlreadyExistsException should store message")
    void userAlreadyExistsExceptionShouldStoreMessage() {
        UserAlreadyExistsException ex = new UserAlreadyExistsException("User exists");

        assertEquals("User exists", ex.getMessage());
    }

    @Test
    @DisplayName("ResourceNotFoundException should store message")
    void resourceNotFoundExceptionShouldStoreMessage() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Not found");

        assertEquals("Not found", ex.getMessage());
    }

    @Test
    @DisplayName("ResourceNotFoundException should format message with resource type and id")
    void resourceNotFoundExceptionShouldFormatMessage() {
        ResourceNotFoundException ex = new ResourceNotFoundException("User", 123);

        assertEquals("User not found with id: 123", ex.getMessage());
    }

    @Test
    @DisplayName("AuthenticationException should store message")
    void authenticationExceptionShouldStoreMessage() {
        AuthenticationException ex = new AuthenticationException("Auth failed");

        assertEquals("Auth failed", ex.getMessage());
    }

    @Test
    @DisplayName("AuthenticationException should store message and cause")
    void authenticationExceptionShouldStoreMessageAndCause() {
        Throwable cause = new RuntimeException("Token expired");
        AuthenticationException ex = new AuthenticationException("Auth failed", cause);

        assertEquals("Auth failed", ex.getMessage());
        assertEquals(cause, ex.getCause());
    }
}

