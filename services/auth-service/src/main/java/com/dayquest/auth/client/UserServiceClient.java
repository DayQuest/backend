package com.dayquest.auth.client;

import com.dayquest.auth.dto.UserServiceDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Feign client for communicating with the User Service.
 */
@FeignClient(name = "user-service", path = "/users")
public interface UserServiceClient {

    @PostMapping
    ResponseEntity<UserServiceDTO> createUser(@RequestBody CreateUserRequest request);

    @PostMapping("/internal/enable/{uuid}")
    ResponseEntity<Void> enableUser(@PathVariable("uuid") UUID uuid);

    @PostMapping("/internal/update-login/{uuid}")
    ResponseEntity<Void> updateLastLogin(@PathVariable("uuid") UUID uuid);

    @GetMapping("/profile/{username}")
    ResponseEntity<UserServiceDTO> getUserByUsername(@PathVariable("username") String username);

    @GetMapping("/{uuid}")
    ResponseEntity<UserServiceDTO> getUserById(@PathVariable("uuid") UUID uuid);

    record CreateUserRequest(String username, String email, String password) {}
}
