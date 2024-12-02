package com.dayquest.dayquestbackend;

import com.dayquest.dayquestbackend.quest.QuestService;
import com.dayquest.dayquestbackend.user.UserRepository;
import com.dayquest.dayquestbackend.user.UserService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.scheduling.annotation.*;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableAsync
@EnableWebSecurity
@EnableScheduling
@EnableTransactionManagement
public class SpringConfiguration implements WebMvcConfigurer{

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
  @PostConstruct
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

  @Bean
  public AsyncTaskExecutor delegatingSecurityContextAsyncTaskExecutor(ThreadPoolTaskExecutor threadPoolTaskExecutor) {
    return new DelegatingSecurityContextAsyncTaskExecutor(threadPoolTaskExecutor);
  }



  @Bean
  UserDetailsService userDetailsService() {
    return username -> userRepository.findByEmail(username);
  }


}
