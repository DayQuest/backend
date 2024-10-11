package com.dayquest.dayquestbackend.report;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Async;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class ReportService {

    @Autowired
    private ReportRepository reportRepository;

    @Async
    public CompletableFuture<Report> createReport(Report report) {
        return CompletableFuture.completedFuture(reportRepository.save(report));
    }

    @Async
    public CompletableFuture<List<Report>> getAllReports() {
        return CompletableFuture.completedFuture(reportRepository.findAll());
    }

    @Async
    public CompletableFuture<Void> deleteReport(Long id) {
        return CompletableFuture.runAsync(() -> {
            if (reportRepository.existsById(id)) {
                reportRepository.deleteById(id);
            } else {
                throw new RuntimeException("Report not found with id: " + id);
            }
        });
    }

    @Async
    public CompletableFuture<Report> getReportById(Long id) {
        return CompletableFuture.supplyAsync(() ->
            reportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Report not found with id: " + id))
        );
    }

    @Async
    public CompletableFuture<List<Report>> getReportsByUserId(Long userId) {
        return CompletableFuture.completedFuture(reportRepository.findByUserId(userId));
    }

    @Async
    public CompletableFuture<List<Report>> getReportsByVideoId(Long videoId) {
        return CompletableFuture.completedFuture(reportRepository.findByVideoId(videoId));
    }
}