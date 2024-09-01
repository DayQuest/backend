package com.example.dayquest.Controller;

import com.example.dayquest.Service.UserService;
import com.example.dayquest.dto.UuidDTO;
import com.example.dayquest.model.User;
import com.example.dayquest.dto.LoginDTO;
import com.example.dayquest.dto.UserDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
    @PostMapping("/ban/{uuid}")
    public ResponseEntity<String> banUser(@RequestBody UuidDTO uuidDto) {
        UUID validUuid = UUID.fromString(uuidDto.getUuid());
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
    public ResponseEntity<String> unbanUser(@RequestBody long id) {
        userService.unbanUser(id);
        return ResponseEntity.ok("User unbanned");
    }
    @PostMapping("/auth")
    public ResponseEntity<String> UUIDAuth(@RequestBody UuidDTO uuidDto) {
        try {
            UUID validUuid = UUID.fromString(uuidDto.getUuid());
            boolean result = userService.UUIDAuth(validUuid);
            if (result) {
                return ResponseEntity.ok("Authentication successful");
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication failed");
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid UUID format");
        }
    }

}
