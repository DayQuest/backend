package com.example.dayquest.Repository;

import com.example.dayquest.model.Quest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportRepository extends JpaRepository<Quest, Long> {
}