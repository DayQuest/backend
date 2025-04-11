package com.dayquest.dayquestbackend.report;

import java.util.Objects;
import java.util.UUID;

import com.dayquest.dayquestbackend.auth.service.JwtService;
import com.dayquest.dayquestbackend.user.UserRepository;
import com.dayquest.dayquestbackend.video.states.SecurityLevel;
import com.dayquest.dayquestbackend.video.models.Video;
import com.dayquest.dayquestbackend.video.repository.VideoRepository;
import org.springframework.beans.factory.annotation.Autowired;
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
    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/create")
    @Async
    public CompletableFuture<ResponseEntity<Report>> createReport(@RequestBody ReportDTO report, @RequestHeader("Authorization") String token) {
        return CompletableFuture.supplyAsync(() -> {
            UUID userId = userRepository.findByUsername(jwtService.extractUsername(token.substring(7))).getUuid();
            if (reportRepository.findByUserIdAndEntityId(userId, report.getEntityId()) != null) {
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }
            if (report.getType() == Type.VIDEO) {
                Video video = videoRepository.findById(report.getEntityId()).orElse(null);
                if (Objects.isNull(video)) {
                    return ResponseEntity.notFound().build();
                }
                if (reportRepository.findByEntityId(report.getEntityId()).size() > 2) {
                    video.setSecurityLevel(SecurityLevel.SUS);
                    videoRepository.save(video);
                } else if (reportRepository.findByEntityId(report.getEntityId()).size() > 5) {
                    video.setSecurityLevel(SecurityLevel.SUS2);
                    videoRepository.save(video);
                }
            }
            Report newReport = new Report();
            newReport.setDescription(report.getDescription());
            newReport.setEntityId(report.getEntityId());
            newReport.setUserId(userId);
            newReport.setType(report.getType());
            reportRepository.save(newReport);
            return ResponseEntity.status(HttpStatus.CREATED).build();
        });
    }

    @GetMapping
    @Async
    public CompletableFuture<ResponseEntity<List<Report>>> getAllReports(@RequestParam int page, @RequestParam int size, @RequestParam Type type) {
        return CompletableFuture.supplyAsync(() -> {
            List<Report> reports = reportRepository.findAllByType(type, PageRequest.of(page, size));
            return ResponseEntity.ok(reports);
        });
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