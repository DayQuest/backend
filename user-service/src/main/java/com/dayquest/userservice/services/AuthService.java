package com.dayquest.userservice.services;

import com.dayquest.userservice.exceptions.InvalidRequestException;
import com.dayquest.userservice.exceptions.UserAlreadyExistsException;
import com.dayquest.userservice.models.User;
import com.dayquest.userservice.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Random;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuthService {

    @Autowired
    PasswordEncoder passwordEncoder;

    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void register(String username, String email, String password) {
            if(!password.matches("^[\\x21-\\x7E]+$")){
                throw new InvalidRequestException("Invalid password");
            }
            if(userRepository.findByEmailIgnoreCase(email).isPresent() || userRepository.findByUsername(username.toLowerCase()) != null){
                throw new UserAlreadyExistsException("User with this email or username already exists");
            }

            User newUser = new User();
            newUser.setUsername(username);
            newUser.setEmail(email);
            newUser.setPassword(passwordEncoder.encode(password));
            newUser.setVerificationCode(generateVerificationCode());
            newUser.setVerificationCodeExpiresAt(LocalDateTime.now().plusHours(1));
            newUser.setEnabled(false);
            newUser.setAuthorities(List.of("ROLE_USER"));

            //TODO: Send verification email
            //TODO: Set Daily Quest

    }

    private String generateVerificationCode() {
        Random random = new Random();
        int code = random.nextInt(900000) + 100000;
        return String.valueOf(code);
    }
}


