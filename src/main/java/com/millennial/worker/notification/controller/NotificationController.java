package com.millennial.worker.notification.controller;

import com.millennial.worker.sse.SseManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/worker/notifications")
@RequiredArgsConstructor
@Slf4j
@org.springframework.web.bind.annotation.CrossOrigin(origins = "*")
public class NotificationController {

    private final SseManager sseManager;
    private final com.millennial.worker.notification.service.NotificationService notificationService;

    @GetMapping(value = "/subscribe/{userId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@PathVariable Long userId) {
        log.info("SSE subscription request received for userId: {}", userId);
        return sseManager.createEmitter(userId);
    }

    @GetMapping("/user/{userId}")
    public java.util.List<com.millennial.worker.notification.entity.WorkerNotificationEntity> getNotifications(@PathVariable Long userId) {
        log.info("Fetching notifications for userId: {}", userId);
        return notificationService.getNotificationsForUser(userId);
    }

    @org.springframework.web.bind.annotation.PutMapping("/{id}/read")
    public com.millennial.worker.notification.entity.WorkerNotificationEntity markRead(@PathVariable Long id) {
        log.info("Marking notification ID: {} as read", id);
        return notificationService.markAsRead(id);
    }

    @org.springframework.web.bind.annotation.PutMapping("/user/{userId}/read-all")
    public void markAllRead(@PathVariable Long userId) {
        log.info("Marking all notifications as read for userId: {}", userId);
        notificationService.markAllAsReadForUser(userId);
    }

    @org.springframework.web.bind.annotation.PostMapping
    public void createNotification(@org.springframework.web.bind.annotation.RequestBody java.util.Map<String, Object> body) {
        log.info("Creating notification via POST request: {}", body);
        Long userId = getLongValue(body.get("userId"));
        Long taskId = getLongValue(body.get("taskId"));
        String type = (String) body.get("type");
        String title = (String) body.get("title");
        String message = (String) body.get("message");

        notificationService.saveAndSendNotification(userId, taskId, type, title, message);
    }

    private Long getLongValue(Object val) {
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).longValue();
        try {
            return Long.parseLong(val.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
