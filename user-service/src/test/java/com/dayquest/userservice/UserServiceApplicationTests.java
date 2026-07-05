package com.dayquest.userservice;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Context load test - requires database connection.
 * This test is skipped by default and only runs when RUN_INTEGRATION_TESTS=true
 */
@SpringBootTest
@ActiveProfiles("test")
@Tag("integration")
@EnabledIfEnvironmentVariable(named = "RUN_INTEGRATION_TESTS", matches = "true")
class UserServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}

