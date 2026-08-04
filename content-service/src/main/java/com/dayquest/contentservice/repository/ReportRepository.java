package com.dayquest.contentservice.repository;

import com.dayquest.contentservice.model.Report;
import com.dayquest.contentservice.model.ReportStatus;
import com.dayquest.contentservice.model.ReportType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReportRepository extends JpaRepository<Report, UUID> {

    Page<Report> findByStatus(ReportStatus status, Pageable pageable);

    Page<Report> findByType(ReportType type, Pageable pageable);

    Page<Report> findByStatusAndType(ReportStatus status, ReportType type, Pageable pageable);

    List<Report> findByEntityId(UUID entityId);

    long countByStatus(ReportStatus status);
}

