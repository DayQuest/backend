package com.dayquest.dayquestbackend.legals;

import com.dayquest.dayquestbackend.beta.KeyRepository;
import com.dayquest.dayquestbackend.user.UserRepository;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/legals")
public class LegalController {

    private static final String AGB_FILE_PATH = "AGBsDayQuest.md";
    private static final String DATENSCHUTZ_FILE_PATH = "DatenschutzDayQuest.md";

    @GetMapping(value = "/agb", produces = "text/markdown")
    @Async
    public CompletableFuture<ResponseEntity<byte[]>> getAgb() {
        return CompletableFuture.supplyAsync(() -> {
            try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(AGB_FILE_PATH)) {
                if (inputStream == null) {
                    return ResponseEntity.notFound().build();
                }
                byte[] content = inputStream.readAllBytes();
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType("text/markdown"))
                        .body(content);
            } catch (Exception e) {
                System.err.println("Error reading AGB file: " + e.getMessage());
                return ResponseEntity.notFound().build();
            }
        });
    }

    @GetMapping(value = "/datenschutz", produces = "text/markdown")
    @Async
    public CompletableFuture<ResponseEntity<byte[]>> getDatenschutz() {
        return CompletableFuture.supplyAsync(() -> {
            try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(DATENSCHUTZ_FILE_PATH)) {
                if (inputStream == null) {
                    return ResponseEntity.notFound().build();
                }
                byte[] content = inputStream.readAllBytes();
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType("text/markdown"))
                        .body(content);
            } catch (Exception e) {
                System.err.println("Error reading Datenschutz file: " + e.getMessage());
                return ResponseEntity.notFound().build();
            }
        });
    }
}