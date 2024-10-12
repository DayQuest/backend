package com.dayquest.dayquestbackend.user;

import com.dayquest.dayquestbackend.quest.QuestRepository;
import com.dayquest.dayquestbackend.quest.Quest;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
  public CompletableFuture<ResponseEntity<String>> registerUser(String username, String email, String password) {
    return CompletableFuture.supplyAsync(() -> {
      if (username == null || email == null || password == null || username.isEmpty()
          || email.isEmpty() || password.isEmpty()) {
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
      }

      if (userRepository.findByUsername(username) != null) {
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body("User with that name already exists");
      }

      User newUser = new User();
      newUser.setUsername(username);
      newUser.setEmail(email);
      newUser.setPassword(passwordEncoder.encode(password));
      newUser.setUuid(UUID.randomUUID());

      userRepository.save(newUser);
      return ResponseEntity.ok("Successfully registered new user");
    });
  }

  @Async
  public CompletableFuture<Boolean> authenticateUser(UUID uuid, String token) {
    return CompletableFuture.supplyAsync(() -> {
      Optional<User> user = userRepository.findById(uuid);
      return user.isPresent() && !user.get().isBanned(); //Add token checker
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
  public CompletableFuture<ResponseEntity<String>> changeBanStatus(UUID uuid, boolean banned) {
    return CompletableFuture.supplyAsync(() -> {
      Optional<User> user = userRepository.findById(uuid);
      if (user.isEmpty()) {
        return ResponseEntity.notFound().build();
      }

      if (user.get().isBanned() == banned) {
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body("User already has ban status: " + banned);
      }
      user.get().setBanned(banned);

      if (banned) {
        user.get().setUsername(user.get().getUsername() + "_banned");
      } else user.get().setUsername(user.get().getUsername().replaceAll("_banned", ""));

      userRepository.save(user.get());
      return ResponseEntity.ok("Ban status changed successfully");
    });
  }


  @Async
  public CompletableFuture<ResponseEntity<String>> updateUserProfile(UUID uuid, String username, String email) {
    return CompletableFuture.supplyAsync(() -> {
      Optional<User> user = userRepository.findById(uuid);
      if (user.isEmpty()) {
        return ResponseEntity.notFound().build();
      }

      user.get().setUsername(username);
      user.get().setEmail(email);
      userRepository.save(user.get());
      return ResponseEntity.ok("Updated successfully");
    });
  }
}