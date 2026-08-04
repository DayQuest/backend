package com.dayquest.userservice.integration;

import com.dayquest.common.jwt.JwtService;
import com.dayquest.userservice.models.User;
import com.dayquest.userservice.repositories.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class UserIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    @Test
    void shouldGetProfileByUsername() throws Exception {
        // Given
        User user = new User();
        user.setUsername("testprofile");
        user.setEmail("testprofile@example.com");
        user.setPassword("hashedpassword");
        user.setCreatedAt(LocalDateTime.now());
        user.setEnabled(true);
        user = userRepository.save(user);

        String token = jwtService.generateToken(user.getUuid());

        // When
        MvcResult getResult = mockMvc.perform(get("/users/profile/" + user.getUsername())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(request().asyncStarted())
                .andReturn();

        // Then
        mockMvc.perform(asyncDispatch(getResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testprofile"));
    }

    @Test
    void shouldSearchUsers() throws Exception {
        // Given
        User user1 = new User();
        user1.setUsername("searchable_one");
        user1.setEmail("one@example.com");
        user1.setPassword("pwd");
        user1.setCreatedAt(LocalDateTime.now());
        user1.setEnabled(true);
        
        User user2 = new User();
        user2.setUsername("searchable_two");
        user2.setEmail("two@example.com");
        user2.setPassword("pwd");
        user2.setCreatedAt(LocalDateTime.now());
        user2.setEnabled(true);
        
        userRepository.save(user1);
        userRepository.save(user2);

        // When
        MvcResult searchResult = mockMvc.perform(get("/users/search")
                .param("query", "searchable"))
                .andExpect(request().asyncStarted())
                .andReturn();

        // Then
        mockMvc.perform(asyncDispatch(searchResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users.length()").value(2))
                .andExpect(jsonPath("$.totalItems").value(2));
    }
}
