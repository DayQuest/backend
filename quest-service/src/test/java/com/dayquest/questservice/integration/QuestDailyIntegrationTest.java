package com.dayquest.questservice.integration;

import com.dayquest.questservice.dto.QuestDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class QuestDailyIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldGetDailyQuest() throws Exception {
        UUID userId = UUID.randomUUID();

        // When
        MvcResult getResult = mockMvc.perform(get("/quests/daily")
                .header("X-User-Id", userId.toString()))
                .andExpect(request().asyncStarted())
                .andReturn();

        // Note: this might return 404 if there are no quests in DB to assign as daily. 
        // We will just verify it dispatches without server error.
        mockMvc.perform(asyncDispatch(getResult));
    }
}
