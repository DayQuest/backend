package com.dayquest.common.config;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MonitoringConfigTest {

    @Test
    void commonTagsShouldIncludeApplicationAndEnvironment() {
        MonitoringConfig monitoringConfig = new MonitoringConfig();
        ReflectionTestUtils.setField(monitoringConfig, "applicationName", "user-service");
        ReflectionTestUtils.setField(monitoringConfig, "environment", "docker,prod");

        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        monitoringConfig.commonTags().customize(meterRegistry);

        meterRegistry.counter("test.counter").increment();

        assertEquals(1.0, meterRegistry.get("test.counter")
                .tag("application", "user-service")
                .tag("environment", "docker")
                .counter()
                .count());
    }
}
