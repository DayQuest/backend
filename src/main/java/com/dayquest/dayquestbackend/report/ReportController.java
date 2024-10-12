package com.dayquest.dayquestbackend.report;

import org.springframework.beans.factory.annotation.Autowired;
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
    private ReportService reportService;

    @PostMapping("/create")
    @Async
    public CompletableFuture<ResponseEntity<Report>> createReport(@RequestBody Report report) {
        return reportService.createReport(report)
            .thenApply(newReport -> ResponseEntity.status(HttpStatus.CREATED).body(newReport));
    }

    @PostMapping("/get")
    @Async
    public CompletableFuture<ResponseEntity<List<Report>>> getAllReports() {
        return reportService.getAllReports()
            .thenApply(ResponseEntity::ok);
    }

    @PostMapping("/delete/{id}")
    @Async
    public CompletableFuture<ResponseEntity<Void>> deleteReport(@PathVariable Long id) {
        return reportService.deleteReport(id)
            .thenApply(result -> ResponseEntity.noContent().<Void>build());
    }
}