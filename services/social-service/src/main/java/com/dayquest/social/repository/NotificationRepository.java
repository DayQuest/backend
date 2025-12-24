package com.dayquest.social.repository;

import com.dayquest.social.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Page<Notification> findByUserUuidOrderByCreatedAtDesc(UUID userUuid, Pageable pageable);

    Page<Notification> findByUserUuidAndReadFalseOrderByCreatedAtDesc(UUID userUuid, Pageable pageable);

    long countByUserUuidAndReadFalse(UUID userUuid);

    void deleteByUserUuid(UUID userUuid);
}
