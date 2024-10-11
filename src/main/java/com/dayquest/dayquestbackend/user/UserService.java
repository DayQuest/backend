package com.dayquest.dayquestbackend.user;

import com.dayquest.dayquestbackend.quest.QuestRepository;
import com.dayquest.dayquestbackend.quest.Quest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Async;

import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private QuestRepository questRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    private final Random random = new Random();

    @Async
    public CompletableFuture<Boolean> registerUser(String username, String email, String password) {
        return CompletableFuture.supplyAsync(() -> {
            if (username == null || email == null || password == null || username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                return false;
            }

            if (userRepository.findByUsername(username) != null) {
                return false;
            }

            User newUser = new User();
            newUser.setUsername(username);
            newUser.setEmail(email);
            newUser.setPassword(passwordEncoder.encode(password));
            newUser.setUuid(UUID.randomUUID());

            userRepository.save(newUser);
            return true;
        });
    }

    @Async
    public CompletableFuture<Boolean> authenticateUser(UUID uuid, String token) {
        return CompletableFuture.supplyAsync(() -> {
            User user = userRepository.findByUuid(uuid);
            return user != null && !user.isBanned(); //Add token checker
        });
    }

    @Async
    public CompletableFuture<Void> assignDailyQuests(List<Quest> topQuests) {
        return CompletableFuture.runAsync(() -> {
            if (topQuests == null || topQuests.isEmpty()) {
                throw new IllegalArgumentException("The list of top quests must not be null or empty");
            }

            List<User> allUsers = userRepository.findAll();
            for (User user : allUsers) {
                Quest randomQuest = topQuests.get(random.nextInt(topQuests.size()));
                user.setDailyQuest(randomQuest);
                userRepository.save(user);
            }
        });
    }

    @Async
    public CompletableFuture<Void> banUser(UUID id) {
        return CompletableFuture.runAsync(() -> {
            User user = userRepository.findByUuid(id);
            if (user != null) {
                user.setBanned(true);
                user.setUsername(user.getUsername() + "_banned");
                userRepository.save(user);
            }
        });
    }

    @Async
    public CompletableFuture<Void> unbanUser(UUID id) {
        return CompletableFuture.runAsync(() -> {
            User user = userRepository.findByUuid(id);
            if (user != null) {
                user.setBanned(false);
                userRepository.save(user);
            }
        });
    }


    @Async
    public CompletableFuture<Boolean> updateUserProfile(UUID uuid, String username, String email) {
        return CompletableFuture.supplyAsync(() -> {
            User user = userRepository.findByUuid(uuid);
            if (user != null) {
                user.setUsername(username);
                user.setEmail(email);
                userRepository.save(user);
                return true;
            }
            return false;
        });
    }
}