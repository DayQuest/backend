package com.dayquest.common.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for metrics and monitoring.
 * All services using the common module will have this configuration.
 */
@Configuration
@ConditionalOnClass(MeterRegistry.class)
public class MonitoringConfig {

    @Value("${spring.application.name:unknown}")
    private String applicationName;

    @Value("${management.metrics.tags.environment:${SPRING_PROFILES_ACTIVE:${spring.profiles.active:default}}}")
    private String environment;

    /**
     * Customizes the MeterRegistry to add common tags to all metrics.
     * This helps identify which service the metrics come from.
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> commonTags() {
        return registry -> registry.config()
                .commonTags(
                        "application", applicationName,
                        "environment", firstActiveProfile(environment)
                );
    }

    private String firstActiveProfile(String configuredEnvironment) {
        if (configuredEnvironment == null || configuredEnvironment.isBlank()) {
            return "default";
        }

        String[] profiles = configuredEnvironment.split(",");
        String firstProfile = profiles[0].trim();
        return firstProfile.isBlank() ? "default" : firstProfile;
    }
}
