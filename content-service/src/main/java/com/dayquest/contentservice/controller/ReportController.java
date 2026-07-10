package com.dayquest.contentservice.controller;

import com.dayquest.contentservice.dto.CreateReportRequest;
import com.dayquest.contentservice.model.Report;
import com.dayquest.contentservice.model.ReportStatus;
import com.dayquest.contentservice.model.ReportType;
import com.dayquest.contentservice.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/reports")
@Tag(name = "Reports", description = "Content reporting endpoints")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping
    @Operation(summary = "Create a new report")
    public ResponseEntity<Report> createReport(
            @Valid @RequestBody CreateReportRequest request,
            @RequestHeader("X-User-Id") String userIdHeader) {

        UUID reporterUuid = UUID.fromString(userIdHeader);
        Report report = reportService.createReport(request, reporterUuid);
        return ResponseEntity.status(HttpStatus.CREATED).body(report);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a report by ID")
    public ResponseEntity<Report> getReport(@PathVariable UUID id) {
        return reportService.getReport(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @Operation(summary = "Get all reports (admin)")
    public ResponseEntity<?> getReports(
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(required = false) ReportType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader(value = "X-User-Roles", defaultValue = "") String roles) {

        if (!roles.contains("ROLE_ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(reportService.getReports(status, type, PageRequest.of(page, size)));
    }

    @PostMapping("/{id}/resolve")
    @Operation(summary = "Resolve a report (admin)")
    public ResponseEntity<?> resolveReport(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-User-Roles", defaultValue = "") String roles) {

        if (!roles.contains("ROLE_ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        UUID modUuid = UUID.fromString(userIdHeader);
        ReportStatus newStatus = ReportStatus.valueOf(body.getOrDefault("status", "RESOLVED"));
        String modMessage = body.get("message");

        boolean resolved = reportService.resolveReport(id, newStatus, modMessage, modUuid);
        if (!resolved) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(Map.of("message", "Report resolved"));
    }

    @GetMapping("/count/open")
    @Operation(summary = "Get open report count")
    public ResponseEntity<Map<String, Long>> getOpenReportCount() {
        return ResponseEntity.ok(Map.of("count", reportService.getOpenReportCount()));
    }
}

