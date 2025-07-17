package com.dayquest.userservice.controllers;

import com.dayquest.userservice.dto.RegisterDTO;
import com.dayquest.userservice.exceptions.InvalidRequestException;
import com.dayquest.userservice.exceptions.UserAlreadyExistsException;
import com.dayquest.userservice.services.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController("/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterDTO registerDTO) {
        try{
            authService.register(registerDTO.getUsername(), registerDTO.getEmail(), registerDTO.getPassword());
            return ResponseEntity.ok("Registered Successfully");
        } catch (InvalidRequestException | UserAlreadyExistsException e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
