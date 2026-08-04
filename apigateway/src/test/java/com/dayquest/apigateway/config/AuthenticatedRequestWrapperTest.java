package com.dayquest.apigateway.config;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AuthenticatedRequestWrapper Tests")
class AuthenticatedRequestWrapperTest {

    @Test
    @DisplayName("Should add custom header")
    void shouldAddCustomHeader() {
        MockHttpServletRequest originalRequest = new MockHttpServletRequest();
        AuthenticatedRequestWrapper wrapper = new AuthenticatedRequestWrapper(originalRequest);

        wrapper.setHeader("X-User-Id", "12345");

        assertEquals("12345", wrapper.getHeader("X-User-Id"));
    }

    @Test
    @DisplayName("Should return original header if custom not set")
    void shouldReturnOriginalHeaderIfCustomNotSet() {
        MockHttpServletRequest originalRequest = new MockHttpServletRequest();
        originalRequest.addHeader("Authorization", "Bearer token");
        AuthenticatedRequestWrapper wrapper = new AuthenticatedRequestWrapper(originalRequest);

        assertEquals("Bearer token", wrapper.getHeader("Authorization"));
    }

    @Test
    @DisplayName("Should override original header with custom")
    void shouldOverrideOriginalHeaderWithCustom() {
        MockHttpServletRequest originalRequest = new MockHttpServletRequest();
        originalRequest.addHeader("X-Custom", "original");
        AuthenticatedRequestWrapper wrapper = new AuthenticatedRequestWrapper(originalRequest);

        wrapper.setHeader("X-Custom", "custom");

        assertEquals("custom", wrapper.getHeader("X-Custom"));
    }

    @Test
    @DisplayName("Should include custom headers in header names")
    void shouldIncludeCustomHeadersInHeaderNames() {
        MockHttpServletRequest originalRequest = new MockHttpServletRequest();
        originalRequest.addHeader("Original-Header", "value");
        AuthenticatedRequestWrapper wrapper = new AuthenticatedRequestWrapper(originalRequest);

        wrapper.setHeader("Custom-Header", "value");

        List<String> headerNames = Collections.list(wrapper.getHeaderNames());
        assertTrue(headerNames.contains("Custom-Header"));
        assertTrue(headerNames.contains("Original-Header"));
    }

    @Test
    @DisplayName("Should return custom header values")
    void shouldReturnCustomHeaderValues() {
        MockHttpServletRequest originalRequest = new MockHttpServletRequest();
        AuthenticatedRequestWrapper wrapper = new AuthenticatedRequestWrapper(originalRequest);

        wrapper.setHeader("X-Roles", "ROLE_USER,ROLE_ADMIN");

        List<String> values = Collections.list(wrapper.getHeaders("X-Roles"));
        assertEquals(1, values.size());
        assertEquals("ROLE_USER,ROLE_ADMIN", values.get(0));
    }
}

