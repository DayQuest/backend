package com.dayquest.dayquestbackend.report;

import jakarta.persistence.Table;

import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, UUID> {
    List<Report> findByEntityId(UUID entityUuid);

    List<Report> findAllByType(Type type, PageRequest pageRequest);

    Report findByUserIdAndEntityId(UUID userId, UUID entityId);
}
