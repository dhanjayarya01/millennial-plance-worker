package com.millennial.worker.redis.consumer;

import com.millennial.worker.email.dto.EmailDto;
import com.millennial.worker.email.service.EmailService;
import com.millennial.worker.notification.service.NotificationService;
import com.millennial.worker.redis.queue.QueueConstants;
import com.millennial.worker.redis.producer.RedisProducer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisConsumer {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ThreadPoolTaskExecutor taskExecutor;
    private final RedisProducer redisProducer;
    private final NotificationService notificationService;
    private final EmailService emailService;

    @PostConstruct
    public void startConsumers() {
        taskExecutor.execute(this::consumeDeadlineReminders);
        taskExecutor.execute(this::consumeNotifications);
        taskExecutor.execute(this::consumeEmails);
    }

    private void consumeDeadlineReminders() {
        log.info("Started Deadline Reminders Queue Consumer loop.");
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Object rawJob = redisTemplate.opsForList().leftPop(QueueConstants.DEADLINE_REMINDERS_QUEUE, 1, TimeUnit.SECONDS);
                if (rawJob instanceof Map) {
                    Map<?, ?> job = (Map<?, ?>) rawJob;
                    log.info("Processing deadline reminder job from queue: {}", job);

                    Long taskId = getLongValue(job.get("taskId"));
                    Long userId = getLongValue(job.get("userId"));
                    String type = (String) job.get("type");
                    String title = (String) job.get("title");
                    String message = (String) job.get("message");
                    String userEmail = (String) job.get("userEmail");

                    Map<String, Object> notifPayload = Map.of(
                            "taskId", taskId,
                            "userId", userId,
                            "type", type,
                            "title", title,
                            "message", message
                    );
                    redisProducer.pushToQueue(QueueConstants.NOTIFICATIONS_QUEUE, notifPayload);

                    if (userEmail != null && !userEmail.isEmpty()) {
                        EmailDto emailDto = new EmailDto();
                        emailDto.setTo(userEmail);
                        emailDto.setSubject(title);
                        emailDto.setHtml(message);
                        redisProducer.pushToQueue(QueueConstants.EMAILS_QUEUE, emailDto);
                    }
                }
            } catch (Exception e) {
                log.error("Error in Deadline Reminders Consumer: {}", e.getMessage());
                sleep(2000);
            }
        }
    }

    private void consumeNotifications() {
        log.info("Started Notifications Queue Consumer loop.");
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Object rawJob = redisTemplate.opsForList().leftPop(QueueConstants.NOTIFICATIONS_QUEUE, 1, TimeUnit.SECONDS);
                if (rawJob instanceof Map) {
                    Map<?, ?> job = (Map<?, ?>) rawJob;
                    log.info("Processing notification job from queue: {}", job);

                    Long userId = getLongValue(job.get("userId"));
                    Long taskId = getLongValue(job.get("taskId"));
                    String type = (String) job.get("type");
                    String title = (String) job.get("title");
                    String message = (String) job.get("message");

                    notificationService.saveAndSendNotification(userId, taskId, type, title, message);
                }
            } catch (Exception e) {
                log.error("Error in Notifications Consumer: {}", e.getMessage());
                sleep(2000);
            }
        }
    }

    private void consumeEmails() {
        log.info("Started Emails Queue Consumer loop.");
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Object rawJob = redisTemplate.opsForList().leftPop(QueueConstants.EMAILS_QUEUE, 1, TimeUnit.SECONDS);
                if (rawJob instanceof Map) {
                    Map<?, ?> job = (Map<?, ?>) rawJob;
                    log.info("Processing email job from queue: {}", job);

                    EmailDto emailDto = new EmailDto();
                    emailDto.setTo((String) job.get("to"));
                    emailDto.setSubject((String) job.get("subject"));
                    emailDto.setHtml((String) job.get("html"));

                    emailService.sendEmailWithRetry(emailDto);
                }
            } catch (Exception e) {
                log.error("Error in Emails Consumer: {}", e.getMessage());
                sleep(2000);
            }
        }
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

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
