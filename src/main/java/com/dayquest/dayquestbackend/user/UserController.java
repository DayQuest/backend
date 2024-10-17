package com.dayquest.dayquestbackend.user;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/users")
public class UserController {

  @Autowired
  private UserService userService;

  @Autowired
  private UserRepository userRepository;

  @PostMapping("/register")
  @Async
  public CompletableFuture<ResponseEntity<String>> registerUser(@RequestBody UserDTO userDTO) {
    return userService.registerUser(userDTO.getUsername(), userDTO.getEmail(),
        userDTO.getPassword());
  }

  @PostMapping("/status")
  public ResponseEntity<Object> status() {

    //TODO: Implement, documentation
    return ResponseEntity.ok().build();
  }

  @PostMapping("/login")
  @Async
  public CompletableFuture<ResponseEntity<LoginResponse>> loginUser(
      @RequestBody LoginDTO loginDTO) {

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

      return ResponseEntity.ok(
          new LoginResponse(user.getUuid(), "Unimplemented", "Login permitted"));
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
  public CompletableFuture<ResponseEntity<String>> authUser(@RequestBody UUID uuid) {
    return CompletableFuture.supplyAsync(() -> {
      if (userService.authenticateUser(uuid, null).join()) {
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

  @GetMapping("{uuid}/videos")
    @Async
    public CompletableFuture<ResponseEntity<Object>> getUserVideos(@PathVariable UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            Optional<User> user = userRepository.findById(uuid);
            return user.<ResponseEntity<Object>>map(value -> ResponseEntity.ok(value.getPostedVideos())).orElseGet(() -> ResponseEntity.notFound().build());
        });
    }
}