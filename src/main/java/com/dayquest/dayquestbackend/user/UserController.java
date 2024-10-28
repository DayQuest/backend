package com.dayquest.dayquestbackend.user;

import java.io.IOException;
import java.util.Optional;

import com.dayquest.dayquestbackend.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/users")
public class UserController {

  @Autowired
  private UserService userService;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private JwtUtil jwtUtil;

  @PostMapping("/register")
  @Async
  public CompletableFuture<ResponseEntity<String>> registerUser(@RequestBody UserDTO userDTO) {
    return userService.registerUser(userDTO.getUsername(), userDTO.getEmail(),
        userDTO.getPassword(), userDTO.getBetaKey());
  }

  @PostMapping("/status")
  public ResponseEntity<Object> status() {

    //TODO: Implement, documentation
    return ResponseEntity.ok().build();
  }

  @PostMapping("/login")
  @Async
  public CompletableFuture<ResponseEntity<LoginResponse>> loginUser(@RequestBody LoginDTO loginDTO) {

    return CompletableFuture.supplyAsync(() -> {
      User user = userRepository.findByUsername(loginDTO.getUsername());

      if (user == null) {
        return ResponseEntity.notFound().build();
      }

      if (user.isBanned()) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                new LoginResponse(null, null, "User has been banned")
        );
      }

      String token = jwtUtil.generateToken(user.getUuid());

      return ResponseEntity.ok(
              new LoginResponse(user.getUuid(), token, "Login permitted")
      );
    });
  }

  @PostMapping("/ban")
  @Async
  public CompletableFuture<ResponseEntity<String>> banUser(@RequestBody UUID uuid) {
    return userService.changeBanStatus(uuid, true);
  }

  @PostMapping("/unban")
  @Async
  public CompletableFuture<ResponseEntity<String>> unbanUser(@RequestBody UUID uuid) {
    return userService.changeBanStatus(uuid, false);
  }

  @PostMapping("/auth")
  @Async
  public CompletableFuture<ResponseEntity<String>> authUser(@RequestBody UUID uuid , @RequestHeader("Authorization") String token) {
    return CompletableFuture.supplyAsync(() -> {
      if (userService.authenticateUser(uuid, null).join()) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token or uuid did not match");
      }

      if(jwtUtil.validateToken(token, uuid)) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token or uuid did not match");
      }

      return ResponseEntity.ok("Authentication successful");
    });
  }

  @PostMapping("/update")
  @Async
  public CompletableFuture<ResponseEntity<String>> updateUser(
      @RequestBody UpdateUserDTO updateUserDTO) {

    return userService.updateUserProfile(updateUserDTO.getUuid(), updateUserDTO.getUsername(),
        updateUserDTO.getEmail());
  }

  @GetMapping("/{uuid}")
  @Async
  public CompletableFuture<ResponseEntity<User>> getUserByUuid(@PathVariable UUID uuid) {
    return CompletableFuture.supplyAsync(() -> {
      Optional<User> user = userRepository.findById(uuid);
      return user.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    });
  }

  @GetMapping("/profile/{username}")
  @Async
  @Transactional(readOnly = true)
  public CompletableFuture<ResponseEntity<ProfileDTO>> getUserByUsername(@PathVariable String username) {
    return CompletableFuture.supplyAsync(() -> {
      User userWithVideos = userRepository.findByUsernameWithVideos(username);
      if (userWithVideos == null) {
        return ResponseEntity.notFound().build();
      }
      ProfileDTO profileDTO = new ProfileDTO(
              userWithVideos.getUsername(),
              userWithVideos.getProfilePicture(),
              userWithVideos.getPostedVideos()
      );
      return ResponseEntity.ok(profileDTO);
    });
  }

  @PostMapping("/setprofilepicture")
  public ResponseEntity<String> setProfilePicture(@RequestParam("file") MultipartFile file, @RequestParam("uuid") UUID uuid) {

    if (file.isEmpty()) {
      return ResponseEntity.badRequest().body("File is empty");
    }

    String fileType = file.getContentType();
    if (!"image/jpeg".equals(fileType) && !"image/png".equals(fileType)) {
      return ResponseEntity.badRequest().body("Only JPG or PNG images are allowed");
    }

    try {
      byte[] fileBytes = file.getBytes();
        Optional<User> user = userRepository.findById(uuid);
        if (user.isEmpty()) {
          return ResponseEntity.ok("User not found");
        }
        user.get().setProfilePicture(fileBytes);
        userRepository.save(user.get());
      return ResponseEntity.ok("Profile picture uploaded successfully");

    } catch (IOException e) {
      return ResponseEntity.status(500).body("Failed to process the file");
    }
  }
}