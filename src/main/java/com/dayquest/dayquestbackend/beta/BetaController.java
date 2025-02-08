package com.dayquest.dayquestbackend.beta;

import java.util.concurrent.CompletableFuture;

import com.dayquest.dayquestbackend.common.dto.DiscordIdDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/beta")
public class BetaController {

  @Autowired
  private KeyFactory keyFactory;

  @Autowired
  private KeyRepository keyRepository;

  @PostMapping("/new-key")
  @Async
  public CompletableFuture<ResponseEntity<String>> newKey(@RequestBody DiscordIdDTO discordIdDTO) {
    return CompletableFuture.supplyAsync(() -> {
      if (keyRepository.existsById(discordIdDTO.getDiscordId())) {
        return ResponseEntity.unprocessableEntity().body("This id has already been bound to a beta key");
      }

      BetaKey key = new BetaKey();
      key.setKey(keyFactory.generateKey());
      key.setDiscordId(discordIdDTO.getDiscordId());
      key.setInUse(false);

      keyRepository.save(key);
      return ResponseEntity.ok(key.getKey());
    });
  }

  @PostMapping("/get-key")
  @Async
  public CompletableFuture<ResponseEntity<String>> getKey(@RequestBody DiscordIdDTO discordIdDTO) {
    return CompletableFuture.supplyAsync(() -> {
      BetaKey key = keyRepository.findById(discordIdDTO.getDiscordId()).orElse(null);
      if (key == null) {
        return ResponseEntity.unprocessableEntity().body("This id doesn't have a beta key");
      }

      return ResponseEntity.ok(key.getKey());
    });
  }

    @PostMapping("/remove-key")
    @Async
    public CompletableFuture<ResponseEntity<String>> removeKey(@RequestBody DiscordIdDTO discordIdDTO) {
        return CompletableFuture.supplyAsync(() -> {
            BetaKey key = keyRepository.findById(discordIdDTO.getDiscordId()).orElse(null);
            if (key == null) {
                return ResponseEntity.unprocessableEntity().body("This id doesn't have a beta key");
            }

            keyRepository.delete(key);
            return ResponseEntity.ok("Key removed");
        });
    }


  @PostMapping("/is-valid")
  @Async
  public CompletableFuture<ResponseEntity<Boolean>> isValid(@RequestBody KeyDTO key) {
    return CompletableFuture.supplyAsync(() -> {

      String betaKey = key.getBetaKey();

      if (!betaKey.contains("-")) {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < betaKey.length(); i++) {
          builder.append(betaKey.charAt(i));

          if ((i + 1) % 4 == 0 && i != betaKey.length() - 1) {
            builder.append("-");
          }
        }
        betaKey = builder.toString();
      }

      boolean exists = keyRepository.existsByKey(betaKey);
      return ResponseEntity.ok(exists);
    });
  }
}