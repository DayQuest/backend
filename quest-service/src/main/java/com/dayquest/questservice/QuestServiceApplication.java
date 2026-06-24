package com.dayquest.questservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.dayquest.questservice", "com.dayquest.common"})
public class QuestServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(QuestServiceApplication.class, args);
    }
}

