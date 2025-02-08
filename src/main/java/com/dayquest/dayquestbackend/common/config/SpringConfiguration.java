package com.dayquest.dayquestbackend.common.config;

import com.dayquest.dayquestbackend.quest.QuestService;
import com.dayquest.dayquestbackend.user.UserRepository;
import com.dayquest.dayquestbackend.user.UserService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.scheduling.annotation.*;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableAsync
@EnableWebSecurity
@EnableScheduling
@EnableTransactionManagement
public class SpringConfiguration implements WebMvcConfigurer {

  @Autowired
  @Lazy
  private UserService userService;

  @Autowired

  private QuestService questService;


  @Autowired
  private UserRepository userRepository;


  @Bean
  public Cache<Integer, String> videoCache() {
    return Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .maximumSize(500)
            .build();
  }

  @Scheduled(cron = "0 0 0 * * ?")
  @Async
  public CompletableFuture<Void> assignDailyQuest() {
    return CompletableFuture.runAsync(() ->
            userService.assignDailyQuests(questService.getTop10PercentQuests().join()).join());
  }

  @Bean
  public PlatformTransactionManager transactionManager(EntityManagerFactory emf) {
    return new JpaTransactionManager(emf);
  }

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/**")
            .allowedOrigins("*")
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*");
  }
}
