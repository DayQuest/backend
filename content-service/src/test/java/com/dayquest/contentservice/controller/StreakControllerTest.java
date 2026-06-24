package com.dayquest.contentservice.controller;

import com.dayquest.contentservice.model.Streak;
import com.dayquest.contentservice.service.StreakService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StreakController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("Streak Controller Tests")
class StreakControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StreakService streakService;

    private Streak testStreak;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();

        testStreak = new Streak();
        testStreak.setId(UUID.randomUUID());
        testStreak.setUserUuid(testUserId);
        testStreak.setCurrentStreak(5);
        testStreak.setLongestStreak(10);
        testStreak.setLastActivityDate(LocalDateTime.now());
    }

    @Test
    @DisplayName("Should record activity successfully")
    void shouldRecordActivity() throws Exception {
        when(streakService.recordActivity(testUserId)).thenReturn(testStreak);

        mockMvc.perform(post("/streaks/record")
                        .header("X-User-Id", testUserId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStreak").value(5))
                .andExpect(jsonPath("$.longestStreak").value(10));
    }

    @Test
    @DisplayName("Should get my streak")
    void shouldGetMyStreak() throws Exception {
        when(streakService.getStreak(testUserId)).thenReturn(Optional.of(testStreak));

        mockMvc.perform(get("/streaks")
                        .header("X-User-Id", testUserId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStreak").value(5))
                .andExpect(jsonPath("$.longestStreak").value(10));
    }

    @Test
    @DisplayName("Should return default when no streak found")
    void shouldReturnDefaultWhenNoStreakFound() throws Exception {
        when(streakService.getStreak(testUserId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/streaks")
                        .header("X-User-Id", testUserId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStreak").value(0))
                .andExpect(jsonPath("$.longestStreak").value(0));
    }

    @Test
    @DisplayName("Should get user streak by ID")
    void shouldGetUserStreakById() throws Exception {
        when(streakService.getStreak(testUserId)).thenReturn(Optional.of(testStreak));

        mockMvc.perform(get("/streaks/user/{userUuid}", testUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStreak").value(5));
    }

    @Test
    @DisplayName("Should get streak leaderboard")
    void shouldGetLeaderboard() throws Exception {
        Page<Streak> streakPage = new PageImpl<>(List.of(testStreak));
        when(streakService.getTopStreaks(any(PageRequest.class))).thenReturn(streakPage);

        mockMvc.perform(get("/streaks/leaderboard")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].currentStreak").value(5));
    }

    @Test
    @DisplayName("Should get longest streak leaderboard")
    void shouldGetLongestStreakLeaderboard() throws Exception {
        Page<Streak> streakPage = new PageImpl<>(List.of(testStreak));
        when(streakService.getTopLongestStreaks(any(PageRequest.class))).thenReturn(streakPage);

        mockMvc.perform(get("/streaks/leaderboard/longest")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].longestStreak").value(10));
    }
}
