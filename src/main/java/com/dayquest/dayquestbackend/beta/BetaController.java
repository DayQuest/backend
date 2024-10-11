package com.dayquest.dayquestbackend.beta;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/beta")
public class BetaController {

  @Autowired
  private KeyCreator keyCreator;

  @Autowired
  private KeyRepository repository;

  @PostMapping("/newKey")
  public ResponseEntity<String> newKey(@RequestBody BetaRequestDTO betaRequestDTO) {
    BetaKey key = new BetaKey();
    key.setKey(keyCreator.generateKey());
    key.setId(betaRequestDTO.getDiscordId());

    if (repository.existsById(betaRequestDTO.getDiscordId())) {
      return ResponseEntity.unprocessableEntity().body("This id has already bind to a beta key");
    }

    repository.save(key);
    return ResponseEntity.ok(key.getKey());
  }

  @PostMapping("/isValid")
  public ResponseEntity<Boolean> isValid(@RequestBody String key) {
    if (!repository.existsByKey(key)) {
      return ResponseEntity.ok(false);
    }

    return ResponseEntity.ok(true);
  }
}
