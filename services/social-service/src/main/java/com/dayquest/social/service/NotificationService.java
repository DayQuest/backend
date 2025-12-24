package com.dayquest.social.service;

import com.dayquest.common.exception.ResourceNotFoundException;
import com.dayquest.social.dto.NotificationDTO;
import com.dayquest.social.model.Notification;
import com.dayquest.social.model.NotificationType;
import com.dayquest.social.repository.NotificationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public Page<NotificationDTO> getNotifications(UUID userUuid, int page, int size) {
        Page<Notification> notifications = notificationRepository.findByUserUuidOrderByCreatedAtDesc(
                userUuid, PageRequest.of(page, size));
        return notifications.map(NotificationDTO::new);
    }

    public Page<NotificationDTO> getUnreadNotifications(UUID userUuid, int page, int size) {
        Page<Notification> notifications = notificationRepository.findByUserUuidAndReadFalseOrderByCreatedAtDesc(
                userUuid, PageRequest.of(page, size));
        return notifications.map(NotificationDTO::new);
    }

    public long getUnreadCount(UUID userUuid) {
        return notificationRepository.countByUserUuidAndReadFalse(userUuid);
    }

    @Transactional
    public NotificationDTO createNotification(UUID userUuid, UUID actorUuid, 
                                              NotificationType type, String message, UUID referenceId) {
        Notification notification = new Notification(userUuid, actorUuid, type, message, referenceId);
        notification = notificationRepository.save(notification);
        return new NotificationDTO(notification);
    }

    @Transactional
    public void markAsRead(UUID notificationUuid) {
        Notification notification = notificationRepository.findById(notificationUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(UUID userUuid) {
        Page<Notification> notifications = notificationRepository.findByUserUuidAndReadFalseOrderByCreatedAtDesc(
                userUuid, PageRequest.of(0, 1000));
        notifications.forEach(notification -> {
            notification.setRead(true);
            notificationRepository.save(notification);
        });
    }

    @Transactional
    public void deleteAllNotifications(UUID userUuid) {
        notificationRepository.deleteByUserUuid(userUuid);
    }
}
