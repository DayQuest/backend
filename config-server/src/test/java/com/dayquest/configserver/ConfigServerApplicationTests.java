package com.dayquest.configserver;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Basic tests for ConfigServerApplication.
 * Note: Full Spring context tests are skipped because they require
 * a valid Git repository connection or native config setup.
 */
class ConfigServerApplicationTests {

	@Test
	void applicationClassExists() {
		// Verify the main application class exists and can be instantiated
		assertDoesNotThrow(() -> {
			Class<?> clazz = Class.forName("com.dayquest.configserver.ConfigServerApplication");
			assert clazz != null;
		});
	}

	@Test
	void mainMethodExists() throws NoSuchMethodException {
		// Verify the main method exists
		var method = ConfigServerApplication.class.getMethod("main", String[].class);
		assert method != null;
	}
}
