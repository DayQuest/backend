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

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    public boolean registerUser(String username, String email, String password) {
        if (username == null || email == null || password == null || username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            return false;
        }

        if (userRepository.findByUsername(username) != null) {
            return false; // Benutzername bereits vergeben
        }

        User newUser = new User();
        newUser.setUsername(username);
        newUser.setEmail(email);
        newUser.setPassword(passwordEncoder.encode(password)); // Passwort verschlüsseln

        userRepository.save(newUser);

        return true;
    }

    public boolean authenticateUser(String username, String password) {
        User user = userRepository.findByUsername(username);
        return user != null && passwordEncoder.matches(password, user.getPassword()); // Passwort überprüfen
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


    public User getUserById(Long id) {
        return userRepository.findById(id).orElse(null);
    }
    public User getUserByUsername(String username)
    {
        return userRepository.findByUsername(username);
    }
}
