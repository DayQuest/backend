package com.dayquest.socialservice.integration;

import com.dayquest.socialservice.model.Friendship;
import com.dayquest.socialservice.model.FriendshipStatus;
import com.dayquest.socialservice.repository.FriendshipRepository;
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
class SocialIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FriendshipRepository friendshipRepository;

    @BeforeEach
    void setUp() {
        friendshipRepository.deleteAll();
    }

    @Test
    void shouldSendFriendRequest() throws Exception {
        UUID requesterUuid = UUID.randomUUID();
        UUID receiverUuid = UUID.randomUUID();

        mockMvc.perform(post("/friends/request/" + receiverUuid)
                .header("X-User-Id", requesterUuid.toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void shouldAcceptFriendRequest() throws Exception {
        UUID requesterUuid = UUID.randomUUID();
        UUID receiverUuid = UUID.randomUUID();

        Friendship friendship = new Friendship();
        friendship.setUserUuid(requesterUuid);
        friendship.setFriendUuid(receiverUuid);
        friendship.setStatus(FriendshipStatus.PENDING);
        friendship.setCreatedAt(LocalDateTime.now());
        friendship.setUpdatedAt(LocalDateTime.now());

        friendship = friendshipRepository.save(friendship);

        mockMvc.perform(post("/friends/" + friendship.getId() + "/accept")
                .header("X-User-Id", receiverUuid.toString()))
                .andExpect(status().isOk());
    }

    @Test
    void shouldGetFriendsList() throws Exception {
        UUID userUuid = UUID.randomUUID();
        UUID friendUuid = UUID.randomUUID();

        Friendship friendship = new Friendship();
        friendship.setUserUuid(userUuid);
        friendship.setFriendUuid(friendUuid);
        friendship.setStatus(FriendshipStatus.ACCEPTED);
        friendship.setCreatedAt(LocalDateTime.now());
        friendship.setUpdatedAt(LocalDateTime.now());

        friendshipRepository.save(friendship);

        mockMvc.perform(get("/friends")
                .header("X-User-Id", userUuid.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].friendUuid").value(friendUuid.toString()));
    }
}
