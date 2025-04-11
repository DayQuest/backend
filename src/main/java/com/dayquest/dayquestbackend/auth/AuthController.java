package com.dayquest.dayquestbackend.auth;


import com.dayquest.dayquestbackend.auth.service.JwtService;
import com.dayquest.dayquestbackend.user.*;
import com.dayquest.dayquestbackend.user.dto.LoginDTO;
import com.dayquest.dayquestbackend.user.dto.LoginResponseDTO;
import com.dayquest.dayquestbackend.user.dto.UserDTO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    UserRepository userRepository;

    @Autowired
    BCryptPasswordEncoder passwordEncoder;

    @Autowired
    JwtService jwtService;

    @Autowired
    UserService userService;

    @PostMapping("/login")
    @Async
    public CompletableFuture<ResponseEntity<LoginResponseDTO>> loginUser(@Valid @RequestBody LoginDTO loginDTO) {
        return CompletableFuture.supplyAsync(() -> {
            User user = userRepository.findByUsername(loginDTO.getUsername());

            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new LoginResponseDTO(null, null, "User not found"));
            }

            if (user.isBanned()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new LoginResponseDTO(null, null, "User has been banned"));
            }

            if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new LoginResponseDTO(null, null, "Invalid password"));
            }

            if (!user.isEnabled()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new LoginResponseDTO(null, null, "User not verified"));
            }

            String token = jwtService.generateToken(user);

            return ResponseEntity.ok(new LoginResponseDTO(user.getUuid(), token, "Login successful"));
        });
    }

    @PostMapping("/register")
    @Async
    public CompletableFuture<ResponseEntity<String>> registerUser(@RequestBody UserDTO userDTO) {
        return userService.registerUser(userDTO.getUsername(), userDTO.getEmail(),
                userDTO.getPassword(), userDTO.getBetaKey());
    }

    @PostMapping("/forgotPassword")
    @Async
    public CompletableFuture<ResponseEntity<String>> forgotPassword(@RequestBody String email) {
        return CompletableFuture.supplyAsync(() -> {
            User user = userRepository.findByEmail(email);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
            }
            userService.sendResetPasswordEmail(user.getEmail());
            return ResponseEntity.ok("Password reset email sent");
        });
    }

    @PostMapping("/resetPassword")
    @Async
    public CompletableFuture<ResponseEntity<String>> resetPassword(@RequestBody ResetPasswordDTO resetPasswordDTO) {
        return CompletableFuture.supplyAsync(() -> {
            User user = userRepository.findByEmail(resetPasswordDTO.getEmail());
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
            }
            if (!resetPasswordDTO.getToken().equals(user.getVerificationCode())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid verification code");
            }
            user.setPassword(passwordEncoder.encode(resetPasswordDTO.getPassword()));
            return ResponseEntity.ok("Password reset");
        });
    }
}
