package com.example.dayquest;

import com.example.dayquest.beta.KeyCreator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DayquestApplication {
	public static void main(String[] args) {
		SpringApplication.run(DayquestApplication.class, args);
		KeyCreator keyCreator = new KeyCreator();
		while (true) {
			keyCreator.generateKey();
		}
	}
}
