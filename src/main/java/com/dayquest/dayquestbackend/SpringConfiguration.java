package com.dayquest.dayquestbackend;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@Configuration
@EnableAsync
@EnableWebSecurity
public class SpringConfiguration {

}
