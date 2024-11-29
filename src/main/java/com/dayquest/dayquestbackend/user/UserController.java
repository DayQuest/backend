package com.dayquest.dayquestbackend.user;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;

import com.dayquest.dayquestbackend.JwtService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
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
  private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;


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
  public CompletableFuture<ResponseEntity<LoginResponse>> loginUser(@Valid @RequestBody LoginDTO loginDTO) {
    return CompletableFuture.supplyAsync(() -> {
      User user = userRepository.findByUsername(loginDTO.getUsername());

      if (user == null) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new LoginResponse(null, null, "User not found"));
      }

      if (user.isBanned()) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new LoginResponse(null, null, "User has been banned"));
      }

      if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new LoginResponse(null, null, "Invalid password"));
      }

      if(!user.isEnabled()) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new LoginResponse(null, null, "User not verified"));
      }

      String token = jwtService.generateToken(user);

      return ResponseEntity.ok(new LoginResponse(user.getUuid(), token, "Login successful"));
    });
  }

    @PostMapping("/verify")
    public ResponseEntity<String> verifyUser(@RequestBody String token) {
        return userService.verifyAccount(token);
    }

    @PostMapping("/resendcode")
    @Async
    public CompletableFuture<ResponseEntity<String>> resendVerificationCode(@RequestBody String email) {
        userService.resendVerificationCode(email);
        return CompletableFuture.completedFuture(ResponseEntity.ok("Verification code resent"));
    }


 /* @PostMapping("/ban")
  @Async
  public CompletableFuture<ResponseEntity<String>> banUser(@RequestBody UUID uuid) {
    return userService.changeBanStatus(uuid, true);
  }

  @PostMapping("/unban")
  @Async
  public CompletableFuture<ResponseEntity<String>> unbanUser(@RequestBody UUID uuid) {
    return userService.changeBanStatus(uuid, false);
  }*/

  @PostMapping("/auth")
  @Async
  public CompletableFuture<ResponseEntity<String>> authUser(@RequestBody UUID uuid , @RequestHeader("Authorization") String token) {
    return auth(uuid, token);
  }

  private CompletableFuture<ResponseEntity<String>> auth(UUID uuid, String token) {
    return CompletableFuture.supplyAsync(() -> {
      if (userService.authenticateUser(uuid, token).join()) {
        return ResponseEntity.ok("User authenticated");
      } else {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
      }
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
  public CompletableFuture<ResponseEntity<ProfileDTO>> getUserByUuid(@PathVariable UUID uuid) {
    return CompletableFuture.supplyAsync(() -> {
      String username = userRepository.findById(uuid).get().getUsername();
      User userWithVideos = userRepository.findByUsernameWithVideos(username);
      if (userWithVideos == null) {
        return ResponseEntity.notFound().build();
      }
      String profilePictureLink = "http://77.90.21.53:8010/api/users/profilepicture/" + username;
      ProfileDTO profileDTO = new ProfileDTO(
              userWithVideos.getUsername(),
              profilePictureLink,
              userWithVideos.getPostedVideos(),
                userWithVideos.getDailyQuest()
      );
      return ResponseEntity.ok(profileDTO);
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
      String profilePictureLink = "http://77.90.21.53:8010/api/users/profilepicture/" + username;
      ProfileDTO profileDTO = new ProfileDTO(
              userWithVideos.getUsername(),
              profilePictureLink,
              userWithVideos.getPostedVideos(),
              userWithVideos.getDailyQuest()
      );
      return ResponseEntity.ok(profileDTO);
    });
  }

  @GetMapping("/profilepicture/{username}")
  @Async
  public CompletableFuture<ResponseEntity<ByteArrayResource>> getDecodedImage(@PathVariable("username") String username) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        User user = userRepository.findByUsername(username);
        if (user == null) {
          return ResponseEntity.notFound().build();
        }

        byte[] imageBytes = user.getProfilePicture();
        ByteArrayResource resource = new ByteArrayResource(imageBytes);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_JPEG);

        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(imageBytes.length)
                .body(resource);

      } catch (IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
      }
    });
  }


  @PostMapping("/setprofilepicture")
  public ResponseEntity<String> setProfilePicture(@RequestParam("file") MultipartFile file, @RequestParam("uuid") UUID uuid) {

    if (file.isEmpty()) {
      return ResponseEntity.badRequest().body("File is empty");
    }

    try {
      byte[] fileBytes = compressImage(file);
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


  public byte[] compressImage(MultipartFile originalFile) throws IOException {
    BufferedImage originalImage = ImageIO.read(originalFile.getInputStream());

    BufferedImage resizedImage = new BufferedImage(360, 360, BufferedImage.TYPE_INT_RGB);

    Graphics2D g2d = resizedImage.createGraphics();
    g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    g2d.drawImage(originalImage, 0, 0, 360, 360, null);
    g2d.dispose();

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ImageIO.write(resizedImage, "jpg", baos);

    return baos.toByteArray();
  }
}