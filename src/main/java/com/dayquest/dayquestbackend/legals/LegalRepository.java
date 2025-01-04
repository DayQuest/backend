package com.dayquest.dayquestbackend.legals;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LegalRepository extends JpaRepository<Legal, UUID> {
}
