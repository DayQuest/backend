package com.dayquest.dayquestbackend.quest;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface QuestReportRepository extends JpaRepository<QuestReport, UUID> {
}
