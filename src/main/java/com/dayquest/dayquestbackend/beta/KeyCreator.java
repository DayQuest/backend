package com.dayquest.dayquestbackend.beta;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class KeyCreator {


  //TODO:
   String generateKey() {
    UUID uuid = UUID.randomUUID();
    String key = uuid.toString().substring(0, 16).replaceAll("-", "");

    return formatKey(key);
  }


  private String formatKey(String rawKey) {
    return rawKey.replaceAll("(.{4})", "$1-").substring(0, rawKey.length() + 2);
  }
}
