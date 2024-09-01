package com.example.dayquest.Service;

import com.example.dayquest.Repository.QuestRepository;
import com.example.dayquest.model.Quest;
import com.example.dayquest.model.User;
import com.example.dayquest.Service.UserService;
import com.example.dayquest.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;
import java.util.UUID;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    public boolean registerUser(String username, String email, String password) {
        // Check for null or empty fields
        if (username == null || email == null || password == null || username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            return false;
        }

        // Check if the username is already taken
        if (userRepository.findByUsername(username) != null) {
            return false; // Username already taken
        }

        // Create a new User object
        User newUser = new User();
        newUser.setUsername(username);
        newUser.setEmail(email);
        newUser.setPassword(passwordEncoder.encode(password));

        // Generate and set a new UUID
        UUID uuid = UUID.randomUUID();
        newUser.setUuid(uuid);

        // Save the new user to the repository
        userRepository.save(newUser);

        return true;
    }

    public boolean authenticateUser(String username, String password) {
        User user = userRepository.findByUsername(username);
        if (user == null || user.isBanned()) {
            return false; // User not found or banned
        }
        return passwordEncoder.matches(password, user.getPassword()); // Check password
    }

    public boolean UUIDAuth(UUID uuid) {
        User user = userRepository.findByUuid(uuid);
        if (user == null || user.isBanned()) {
            return false; // User not found or banned
        }
        return true;
    }

    @Autowired
    private QuestRepository questRepository;

    private final Random random = new Random();

    public void assignDailyQuests(List<Quest> topQuests) {
        if (topQuests == null || topQuests.isEmpty()) {
            throw new IllegalArgumentException("The list of top quests must not be null or empty");
        }

        List<User> allUsers = userRepository.findAll();

        for (User user : allUsers) {
            // Select a random quest
            Quest randomQuest = topQuests.get(random.nextInt(topQuests.size()));
            user.setDailyQuest(randomQuest);
            userRepository.save(user);
        }
    }

    public void banUser(UUID id) {
        User user = userRepository.findByUuid(id);
        if (user != null) {
            user.setBanned(true);
            userRepository.save(user);
        }
    }
    public void unbanUser(Long id) {
        User user = userRepository.findById(id).orElse(null);
        if (user != null) {
            user.setBanned(false);
            userRepository.save(user);
        }
    }

    public User getUserById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public User getUserByUuid(UUID uuid) {
        return userRepository.findByUuid(uuid);
    }
}
