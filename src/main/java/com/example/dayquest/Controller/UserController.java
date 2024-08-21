package com.example.dayquest.Controller;

import com.example.dayquest.Service.UserService;
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
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.singletonMap("message", "Invalid credentials"));
        }
    }
}
