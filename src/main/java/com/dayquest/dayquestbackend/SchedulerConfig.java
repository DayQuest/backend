package com.dayquest.dayquestbackend;

import com.dayquest.dayquestbackend.quest.QuestService;
import com.dayquest.dayquestbackend.user.UserService;
import jakarta.annotation.PostConstruct;
import java.util.concurrent.CompletableFuture;
import org.hibernate.boot.internal.BootstrapContextImpl;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.beans.factory.annotation.Autowired;

@Configuration
@EnableScheduling
public class SchedulerConfig {

  @Autowired
  private UserService userService;

  @Autowired
  private QuestService questService;

  @PostConstruct
  @Async
  public CompletableFuture<Void> init() {
    return userService.assignDailyQuests(questService.getTop10PercentQuests().join());
  }

  @Scheduled(cron = "0 0 0 * * ?")
  @Async
  public CompletableFuture<Void> assignDailyQuest() {
    return userService.assignDailyQuests(questService.getTop10PercentQuests().join());
  }
}
