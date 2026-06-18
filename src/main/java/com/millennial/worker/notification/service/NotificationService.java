package com.millennial.worker.notification.service;

import com.millennial.worker.notification.entity.WorkerNotificationEntity;
import com.millennial.worker.notification.repository.WorkerNotificationRepository;
import com.millennial.worker.sse.SseManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final WorkerNotificationRepository notificationRepository;
    private final SseManager sseManager;

    @Transactional
    public void saveAndSendNotification(Long userId, Long taskId, String type, String title, String message) {
        log.info("Processing notification for userId: {}, taskId: {}, type: {}", userId, taskId, type);

        if (notificationRepository.existsByUserIdAndTaskIdAndType(userId, taskId, type)) {
            log.info("Notification of type {} already exists for userId: {} and taskId: {}. Skipping.", type, userId, taskId);
            return;
        }

        WorkerNotificationEntity notification = WorkerNotificationEntity.builder()
                .userId(userId)
                .taskId(taskId)
                .type(type)
                .title(title)
                .message(message)
                .isRead(false)
                .build();

        WorkerNotificationEntity saved = notificationRepository.save(notification);
        log.info("Saved notification ID: {}", saved.getId());

        sseManager.sendNotification(userId, saved);
    }

    @Transactional
    public void sendBulkNotifications(List<WorkerNotificationEntity> notifications) {
        log.info("Processing bulk notification dispatch of size: {}", notifications.size());
        for (WorkerNotificationEntity notif : notifications) {
            try {
                saveAndSendNotification(notif.getUserId(), notif.getTaskId(), notif.getType(), notif.getTitle(), notif.getMessage());
            } catch (Exception e) {
                log.error("Failed to process notification in bulk dispatch: {}", e.getMessage());
            }
        }
    }

    @Transactional(readOnly = true)
    public List<WorkerNotificationEntity> getNotificationsForUser(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public WorkerNotificationEntity markAsRead(Long notificationId) {
        return notificationRepository.findById(notificationId)
                .map(notif -> {
                    notif.setRead(true);
                    return notificationRepository.save(notif);
                })
                .orElseThrow(() -> new IllegalArgumentException("Notification not found with ID: " + notificationId));
    }

    @Transactional
    public void markAllAsReadForUser(Long userId) {
        List<WorkerNotificationEntity> unread = notificationRepository.findByUserIdAndIsReadOrderByCreatedAtDesc(userId, false);
        for (WorkerNotificationEntity notif : unread) {
            notif.setRead(true);
        }
        notificationRepository.saveAll(unread);
    }
}
