package com.dayquest.contentservice.integration;

import com.dayquest.contentservice.model.Streak;
import com.dayquest.contentservice.repository.StreakRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@AutoConfigureMockMvc
class ContentIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StreakRepository streakRepository;

    @BeforeEach
    void setUp() {
        streakRepository.deleteAll();
    }

    @Test
    void shouldRecordStreakActivity() throws Exception {
        UUID userUuid = UUID.randomUUID();

        mockMvc.perform(post("/streaks/record")
                .header("X-User-Id", userUuid.toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStreak").value(1))
                .andExpect(jsonPath("$.longestStreak").value(1));
    }

    @Test
    void shouldGetMyStreak() throws Exception {
        UUID userUuid = UUID.randomUUID();

        Streak streak = new Streak();
        streak.setUserUuid(userUuid);
        streak.setCurrentStreak(5);
        streak.setLongestStreak(10);
        streak.setLastActivityDate(LocalDateTime.now());
        streakRepository.save(streak);

        mockMvc.perform(get("/streaks")
                .header("X-User-Id", userUuid.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStreak").value(5))
                .andExpect(jsonPath("$.longestStreak").value(10));
    }
}
