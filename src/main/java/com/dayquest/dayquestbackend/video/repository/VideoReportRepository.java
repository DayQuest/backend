package com.dayquest.dayquestbackend.video.repository;

import com.dayquest.dayquestbackend.video.models.VideoReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface VideoReportRepository extends JpaRepository<VideoReport, UUID> {
}
