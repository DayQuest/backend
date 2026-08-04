package com.dayquest.contentservice.service;

import com.dayquest.contentservice.dto.CreateReportRequest;
import com.dayquest.contentservice.model.Report;
import com.dayquest.contentservice.model.ReportStatus;
import com.dayquest.contentservice.model.ReportType;
import com.dayquest.contentservice.repository.ReportRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class ReportService {

    private final ReportRepository reportRepository;

    public ReportService(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    @Transactional
    public Report createReport(CreateReportRequest request, UUID reporterUuid) {
        Report report = new Report();
        report.setDescription(request.getDescription());
        report.setEntityId(request.getEntityId());
        report.setType(request.getType());
        report.setReason(request.getReason());
        report.setReporterUuid(reporterUuid);
        report.setStatus(ReportStatus.OPEN);
        return reportRepository.save(report);
    }

    public Optional<Report> getReport(UUID reportId) {
        return reportRepository.findById(reportId);
    }

    public Page<Report> getReports(ReportStatus status, ReportType type, Pageable pageable) {
        if (status != null && type != null) {
            return reportRepository.findByStatusAndType(status, type, pageable);
        } else if (status != null) {
            return reportRepository.findByStatus(status, pageable);
        } else if (type != null) {
            return reportRepository.findByType(type, pageable);
        }
        return reportRepository.findAll(pageable);
    }

    @Transactional
    public boolean resolveReport(UUID reportId, ReportStatus newStatus, String modMessage, UUID modUuid) {
        Optional<Report> reportOpt = reportRepository.findById(reportId);
        if (reportOpt.isEmpty()) {
            return false;
        }

        Report report = reportOpt.get();
        report.setStatus(newStatus);
        report.setModMessage(modMessage);
        report.setResolvedByUuid(modUuid);
        report.setResolvedAt(LocalDateTime.now());
        reportRepository.save(report);
        return true;
    }

    public long getOpenReportCount() {
        return reportRepository.countByStatus(ReportStatus.OPEN);
    }
}

