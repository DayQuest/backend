package com.example.dayquest.beta;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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
  public ResponseEntity<String> getKey(@RequestBody BetaRequestDTO betaRequestDTO) {
    BetaKey key = new BetaKey();
    key.setKey(keyCreator.generateKey());

    if (repository.findById(betaRequestDTO.getDiscordId()).isPresent()) {
      return ResponseEntity.unprocessableEntity().body("This id already has a beta key");
    }

    return ResponseEntity.ok(key.getKey());
  }
}
