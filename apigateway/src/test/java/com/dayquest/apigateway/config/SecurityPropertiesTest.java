package com.dayquest.apigateway.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SecurityProperties Tests")
class SecurityPropertiesTest {

    @Test
    @DisplayName("Should have default public paths after init")
    void shouldHaveDefaultPublicPaths() {
        SecurityProperties properties = new SecurityProperties();
        // Simulate Spring's @PostConstruct behavior
        properties.init();

        List<String> publicPaths = properties.getPublicPaths();

        assertNotNull(publicPaths);
        assertTrue(publicPaths.contains("/auth/login"));
        assertTrue(publicPaths.contains("/auth/register"));
        assertTrue(publicPaths.contains("/auth/refresh"));
        assertTrue(publicPaths.contains("/auth/verify"));
        assertTrue(publicPaths.contains("/auth/forgot-password"));
        assertTrue(publicPaths.contains("/auth/reset-password"));
        assertTrue(publicPaths.contains("/actuator/**"));
    }

    @Test
    @DisplayName("Should allow setting custom public paths")
    void shouldAllowSettingCustomPublicPaths() {
        SecurityProperties properties = new SecurityProperties();
        List<String> customPaths = List.of("/custom/path", "/another/path");

        properties.setPublicPaths(customPaths);

        assertEquals(customPaths, properties.getPublicPaths());
    }
}

