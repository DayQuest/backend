package com.dayquest.dayquestbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.core.context.SecurityContextHolder;

@SpringBootApplication
public class DayQuestApplication {
	public static void main(String[] args) {
		SpringApplication.run(DayQuestApplication.class, args);
	}
}
