package com.dayquest.dayquestbackend.report;

import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    @Autowired
    private ReportRepository reportRepository;

    @PostMapping("/create")
    @Async
    public CompletableFuture<ResponseEntity<Report>> createReport(@RequestBody Report report) {
        return CompletableFuture.supplyAsync(() -> {
            reportRepository.save(report);
            return ResponseEntity.status(HttpStatus.CREATED).build();
        });
    }

    @PostMapping("/get")
    @Async
    public CompletableFuture<ResponseEntity<List<Report>>> getAllReports(@RequestParam int page, @RequestParam int size) {
        return CompletableFuture.supplyAsync(() -> ResponseEntity.ok(reportRepository.findAll(PageRequest.of(page, size)).getContent()));
    }

    @PostMapping("/delete/{id}")
    @Async
    public CompletableFuture<ResponseEntity<?>> deleteReport(@PathVariable UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
          if (reportRepository.findById(uuid).isEmpty()) {
            return ResponseEntity.notFound().build();
          }

            reportRepository.deleteById(uuid);
            return ResponseEntity.ok("Deleted report");
        });
    }
}