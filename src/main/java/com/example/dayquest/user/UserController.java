package com.example.dayquest.user;

import com.example.dayquest.util.UuidDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@RequestBody UserDTO userDTO) {
        boolean result = userService.registerUser(
                userDTO.getUsername(),
                userDTO.getEmail(),
                userDTO.getPassword()
        );

        if (result) {
            return ResponseEntity.ok("Registration successful");
        } else {
            return ResponseEntity.badRequest().body("Registration failed");
        }
    }

    @PostMapping("/status")
    public HttpEntity<Object> status() {
        return ResponseEntity.ok().build();
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> loginUser(@RequestBody LoginDTO loginDTO) {
        boolean isAuthenticated = userService.authenticateUser(loginDTO.getUsername(), loginDTO.getPassword());
        if (isAuthenticated) {
            User user = userService.getUserByUsername(loginDTO.getUsername());
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Login successful");
            response.put("userId", user.getId());
            response.put("uuid", user.getUuid().toString());
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.singletonMap("message", "Invalid credentials"));
        }
    }
    @PostMapping("/ban")
    public ResponseEntity<String> banUser(@RequestBody UuidDTO uuidDto) {
        String uuidString = uuidDto.getUuid();

        // Check if the UUID is already formatted
        if (!uuidString.contains("-")) {
            // Insert hyphens to create a valid UUID string
            uuidString = String.format(
                    "%s-%s-%s-%s-%s",
                    uuidString.substring(0, 8),
                    uuidString.substring(8, 12),
                    uuidString.substring(12, 16),
                    uuidString.substring(16, 20),
                    uuidString.substring(20)
            );
        }

        UUID validUuid;
        try {
            validUuid = UUID.fromString(uuidString);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid UUID format");
        }

        if(userService.getUserByUuid(validUuid) == null) {
            return ResponseEntity.badRequest().body("User not found");
        }
        else if(userService.getUserByUuid(validUuid).isBanned()) {
            return ResponseEntity.badRequest().body("User is already banned");
        }
        else {
            userService.banUser(validUuid);
            return ResponseEntity.ok("User banned");
        }
    }
    @PostMapping("/unban")
    public ResponseEntity<String> unbanUser(@RequestBody UuidDTO uuidDto) {
        String uuidString = uuidDto.getUuid();

        // Check if the UUID is already formatted
        if (!uuidString.contains("-")) {
            // Insert hyphens to create a valid UUID string
            uuidString = String.format(
                    "%s-%s-%s-%s-%s",
                    uuidString.substring(0, 8),
                    uuidString.substring(8, 12),
                    uuidString.substring(12, 16),
                    uuidString.substring(16, 20),
                    uuidString.substring(20)
            );
        }

        UUID validUuid;
        try {
            validUuid = UUID.fromString(uuidString);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid UUID format");
        }

        if(userService.getUserByUuid(validUuid) == null) {
            return ResponseEntity.badRequest().body("User not found");
        }
        else if(!userService.getUserByUuid(validUuid).isBanned()) {
            return ResponseEntity.badRequest().body("User is not banned");
        }
        else {
            userService.unbanUser(validUuid);
            return ResponseEntity.ok("User unbanned");
        }
    }
    @Async
    @PostMapping("/auth")
    public CompletableFuture<ResponseEntity<String>> UUIDAuth(@RequestBody UuidDTO uuidDto) {
        try {
            UUID validUuid = UUID.fromString(uuidDto.getUuid());
            boolean result = userService.UUIDAuth(validUuid);
            if (result) {
                return CompletableFuture.completedFuture(ResponseEntity.ok("Authentication successful"));
            } else {
                return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication failed"));
            }
        } catch (IllegalArgumentException e) {
            return CompletableFuture.completedFuture(ResponseEntity.badRequest().body("Invalid UUID format"));
        }
    }
    @PostMapping("/update")
    public ResponseEntity<String> updateUser(@RequestBody UpdateUserDTO updateUserDTO) {
        boolean result = userService.updateUserProfile(
                UUID.fromString(updateUserDTO.getUuid()),
                updateUserDTO.getUsername(),
                updateUserDTO.getEmail()
        );

        if (result) {
            return ResponseEntity.ok("Update successful");
        } else {
            return ResponseEntity.badRequest().body("Update failed");
        }
    }
    @GetMapping("/{uuid}")
    public ResponseEntity<User> getUserByUuid(@PathVariable String uuid) {
        try {
            UUID validUuid = UUID.fromString(uuid);
            User user = userService.getUserByUuid(validUuid);
            if (user != null) {
                return ResponseEntity.ok(user);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

}
