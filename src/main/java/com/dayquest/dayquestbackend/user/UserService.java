package com.dayquest.dayquestbackend.user;

import com.dayquest.dayquestbackend.EmailService;
import com.dayquest.dayquestbackend.JwtService;
import com.dayquest.dayquestbackend.beta.BetaKey;
import com.dayquest.dayquestbackend.beta.KeyRepository;
import com.dayquest.dayquestbackend.quest.QuestRepository;
import com.dayquest.dayquestbackend.quest.Quest;

import java.time.LocalDateTime;
import java.util.Optional;

import com.dayquest.dayquestbackend.quest.QuestService;
import com.dayquest.dayquestbackend.video.Video;
import jakarta.mail.MessagingException;
import jakarta.transaction.Transactional;
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
  private JwtService jwtService;

  @Autowired
  private BCryptPasswordEncoder passwordEncoder;

  @Autowired
  private KeyRepository keyRepository;

    @Autowired
    private EmailService emailService;

  private final Random random = new Random();
    @Autowired
    private QuestService questService;


  @Async
  public CompletableFuture<ResponseEntity<String>> registerUser(String username, String email, String password, String betaKey) {
    return CompletableFuture.supplyAsync(() -> {
      if (username == null || email == null || password == null || username.isEmpty()
          || email.isEmpty() || password.isEmpty() || betaKey == null || betaKey.isEmpty()) {
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
      }

      if (userRepository.findByUsername(username) != null) {
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body("User with that name already exists");
      }

        if (userRepository.findByEmail(email) != null) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body("User with that email already exists");
        }

        if (!keyRepository.existsByKey(betaKey)) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body("Invalid beta key");
        }

        if(keyRepository.findByKey(betaKey).isInUse()) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body("Beta key already in use");
        }

      BetaKey key = keyRepository.findByKey(betaKey);
      key.setInUse(true);
      key.setUsername(username);
      keyRepository.save(key);
      List<Quest> topQuests = questService.getTop10PercentQuests().join();
      Quest randomQuest = topQuests.get(random.nextInt(topQuests.size()));

      User newUser = new User();
      newUser.setUsername(username);
      newUser.setEmail(email);
      newUser.setPassword(passwordEncoder.encode(password));
      newUser.setUuid(UUID.randomUUID());
      newUser.setBetaKey(betaKey);
      newUser.setVerificationCode(generateVerificationCode());
      newUser.setVerificationCodeExpiresAt(LocalDateTime.now().plusHours(1));
      newUser.setEnabled(false);
      sendVerificationEmail(newUser);
      newUser.setDailyQuest(randomQuest);
      userRepository.save(newUser);
      return ResponseEntity.ok("Successfully registered new user");

    });
  }

  @Async
  public CompletableFuture<Boolean> authenticateUser(UUID uuid, String token) {
    return CompletableFuture.supplyAsync(() -> {
      Optional<User> user = userRepository.findById(uuid);
      if(user.isEmpty() || user.get().isBanned()) {
        return false;
      }
        return jwtService.isTokenValid(token, user.get());
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
        Quest lastQuest = user.getDailyQuest();
        Quest randomQuest = topQuests.get(random.nextInt(topQuests.size()));
        while (randomQuest.equals(lastQuest) && topQuests.size() > 1) {
          randomQuest = topQuests.get(random.nextInt(topQuests.size()));
        }
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

  public void resendVerificationCode(String email) {
    Optional<User> optionalUser = userRepository.findByEmailIgnoreCase(email);
    if (optionalUser.isPresent()) {
      User user = optionalUser.get();
      if (user.isEnabled()) {
        throw new RuntimeException("Account is already verified");
      }
      user.setVerificationCode(generateVerificationCode());
      user.setVerificationCodeExpiresAt(LocalDateTime.now().plusHours(1));
      sendVerificationEmail(user);
      userRepository.save(user);
    } else {
      throw new RuntimeException("User not found");
    }
  }

  private void sendVerificationEmail(User user) {
    String subject = "Account Verification";
    String verificationCode = user.getVerificationCode();
    String htmlMessage = "<html>"
            + "<body style=\"font-family: Arial, sans-serif;\">"
            + "<div style=\"background-color: #f5f5f5; padding: 20px;\">"
            + "<h2 style=\"color: #333;\">Willkommen bei DayQuest!</h2>"
            + "<p style=\"font-size: 16px;\">Hier ist dein Code:</p>"
            + "<div style=\"background-color: #fff; padding: 20px; border-radius: 5px; box-shadow: 0 0 10px rgba(0,0,0,0.1);\">"
            + "<h3 style=\"color: #333;\">Verification Code:</h3>"
            + "<p style=\"font-size: 18px; font-weight: bold; color: #007bff;\">" + verificationCode + "</p>"
            + "</div>"
            + "</div>"
            + "</body>"
            + "</html>";

    try {
      emailService.sendVerificationEmail(user.getEmail(), subject, htmlMessage);
    } catch (MessagingException e) {
      e.printStackTrace();
    }
  }

  public ResponseEntity<String> verifyAccount(String verificationCode) {
    System.out.println(verificationCode);
      User user = userRepository.findByVerificationCode(verificationCode);
      if (user!=null) {
        if (user.getVerificationCode().equals(verificationCode)) {
          user.setEnabled(true);
          user.setVerificationCode(null);
          user.setVerificationCodeExpiresAt(null);
          userRepository.save(user);
          return ResponseEntity.ok("Account verified successfully");
        } else {
          return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body("Invalid verification code");
        }
      } else {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
      }
  }

  private String generateVerificationCode() {
    Random random = new Random();
    int code = random.nextInt(900000) + 100000;
    return String.valueOf(code);
  }
}