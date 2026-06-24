package com.dayquest.contentservice.model;

import jakarta.persistence.*;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "reports", indexes = {
    @Index(name = "idx_report_entity", columnList = "entityId"),
    @Index(name = "idx_report_reporter", columnList = "reporterUuid"),
    @Index(name = "idx_report_status", columnList = "status"),
    @Index(name = "idx_report_type", columnList = "type"),
    @Index(name = "idx_report_created", columnList = "createdAt DESC"),
    @Index(name = "idx_report_status_created", columnList = "status, createdAt DESC")
})

@lombok.Getter
@lombok.Setter
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
@lombok.Builder
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Version
    private Long version;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false)
    private UUID entityId;

    @Column(nullable = false)
    private UUID reporterUuid;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ReportStatus status = ReportStatus.OPEN;

    @Enumerated(EnumType.STRING)
    private ReportReason reason;

    @CreationTimestamp
    private LocalDateTime createdAt;

    private LocalDateTime resolvedAt;

    private UUID resolvedByUuid;

    private String modMessage;

    // Getters and Setters











































}

