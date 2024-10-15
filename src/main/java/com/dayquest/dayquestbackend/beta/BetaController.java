package com.dayquest.dayquestbackend.beta;

import java.util.concurrent.CompletableFuture;
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
  public CompletableFuture<ResponseEntity<String>> newKey(@RequestBody long discordId) {
    return CompletableFuture.supplyAsync(() -> {
      if (keyRepository.existsById(discordId)) {
        return ResponseEntity.unprocessableEntity().body("This id has already been bound to a beta key");
      }

      BetaKey key = new BetaKey();
      key.setKey(keyFactory.generateKey());
      key.setDiscordId(discordId);

      keyRepository.save(key);
      return ResponseEntity.ok(key.getKey());
    });
  }

  @PostMapping("/is-valid")
  @Async
  public CompletableFuture<ResponseEntity<Boolean>> isValid(@RequestBody String key) {
    return CompletableFuture.supplyAsync(() -> {
      boolean exists = keyRepository.existsByKey(key);
      return ResponseEntity.ok(exists);
    });
  }
}