package com.dayquest.dayquestbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties
public class DayquestApplication {
	public static void main(String[] args) {
		SpringApplication.run(DayquestApplication.class, args);
	}
}
