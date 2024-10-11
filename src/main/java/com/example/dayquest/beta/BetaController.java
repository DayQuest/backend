package com.example.dayquest.beta;


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
  public ResponseEntity<String> getNewKey(@RequestBody BetaRequestDTO betaRequestDTO) {
    return null;
  }
}
