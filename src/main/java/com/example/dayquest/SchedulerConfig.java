package com.example.dayquest;

import com.example.dayquest.service.QuestService;
import com.example.dayquest.service.UserService;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
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
    public void init() {
        userService.assignDailyQuests(questService.getTop10PercentQuests());
    }

    @Scheduled(cron = "0 0 0 * * ?")
    public void assignDailyQuest() {
        userService.assignDailyQuests(questService.getTop10PercentQuests());
    }


}
