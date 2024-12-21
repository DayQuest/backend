package com.dayquest.dayquestbackend.beta;
import java.util.Random;
import org.springframework.stereotype.Service;

@Service
public class KeyFactory {
  private final Random random = new Random();
  private final char[] options = {
          'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N',
          'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
          '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'
  };

  String generateKey() {
    StringBuilder builder = new StringBuilder();

    for (int i = 0; i < 12; i++) {
      char randomChar = options[random.nextInt(options.length)];

      if (i % 4 == 0 && i != 0) {
        builder.append('-');
      }

      builder.append(randomChar);
    }
    return builder.toString();
  }
}