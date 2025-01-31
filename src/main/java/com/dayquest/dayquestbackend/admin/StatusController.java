package com.dayquest.dayquestbackend.admin;

import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

@RestController
public class StatusController {
    @GetMapping
    @Async
    public CompletableFuture<ResponseEntity<String>> status() {
        return CompletableFuture.completedFuture(ResponseEntity.ok().build());
    }
}
