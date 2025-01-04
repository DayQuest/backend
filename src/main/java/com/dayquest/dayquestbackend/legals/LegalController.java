package com.dayquest.dayquestbackend.legals;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/legals")
public class LegalController {

    private static final String AGB_FILE_PATH = "/app/AGBsDayQuest.md/";
    private static final String DATENSCHUTZ_FILE_PATH = "/app/DatenschutzDayQuest.md/";

    @GetMapping(value = "/agb", produces = "text/markdown")
    @Async
    public CompletableFuture<ResponseEntity<byte[]>> getAgb() {
        return CompletableFuture.supplyAsync(() -> {
            Path path = Paths.get(AGB_FILE_PATH);
            try {
                byte[] content = Files.readAllBytes(path);
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType("text/markdown"))
                        .body(content);
            } catch (IOException e) {
                System.out.println(e);
                return ResponseEntity.notFound().build();
            }
        });
    }

    @GetMapping(value = "/datenschutz", produces = "text/markdown")
    @Async
    public CompletableFuture<ResponseEntity<byte[]>> getDatenschutz() {
        return CompletableFuture.supplyAsync(() -> {
            Path path = Paths.get(DATENSCHUTZ_FILE_PATH);
            try {
                byte[] content = Files.readAllBytes(path);
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType("text/markdown"))
                        .body(content);
            } catch (IOException e) {
                System.out.println(e);
                return ResponseEntity.notFound().build();
            }
        });
    }
}