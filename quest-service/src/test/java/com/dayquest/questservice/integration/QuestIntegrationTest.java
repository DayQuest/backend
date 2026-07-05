package com.dayquest.questservice.integration;

import com.dayquest.questservice.dto.CreateQuestDTO;
import com.dayquest.questservice.dto.QuestDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class QuestIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCreateAndRetrieveQuest() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();
        CreateQuestDTO createDTO = new CreateQuestDTO();
        createDTO.setTitle("Integration Test Quest");
        createDTO.setDescription("A quest created during an integration test");

        // When (Create) - Note: Controller methods are async (CompletableFuture)
        MvcResult createResult = mockMvc.perform(post("/quests")
                .header("X-User-Id", userId.toString())
                .header("X-User-Name", "testuser")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(request().asyncStarted())
                .andReturn();

        MvcResult createDispatchResult = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch(createResult))
                .andExpect(status().isCreated())
                .andReturn();

        String createContent = createDispatchResult.getResponse().getContentAsString();
        QuestDTO createdQuest = objectMapper.readValue(createContent, QuestDTO.class);
        
        assertThat(createdQuest.getUuid()).isNotNull();
        assertThat(createdQuest.getTitle()).isEqualTo("Integration Test Quest");
        assertThat(createdQuest.getCreatorUuid()).isEqualTo(userId);

        // When (Retrieve)
        MvcResult getResult = mockMvc.perform(get("/quests/" + createdQuest.getUuid())
                .header("X-User-Id", userId.toString()))
                .andExpect(request().asyncStarted())
                .andReturn();

        MvcResult getDispatchResult = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch(getResult))
                .andExpect(status().isOk())
                .andReturn();

        String getContent = getDispatchResult.getResponse().getContentAsString();
        QuestDTO retrievedQuest = objectMapper.readValue(getContent, QuestDTO.class);

        assertThat(retrievedQuest.getUuid()).isEqualTo(createdQuest.getUuid());
        assertThat(retrievedQuest.getTitle()).isEqualTo("Integration Test Quest");
    }
}
