package com.millennial.worker.scheduler;

import com.millennial.worker.common.LookupTaskEntity;
import com.millennial.worker.common.LookupTaskRepository;
import com.millennial.worker.common.LookupUserEntity;
import com.millennial.worker.notification.repository.WorkerNotificationRepository;
import com.millennial.worker.redis.producer.RedisProducer;
import com.millennial.worker.redis.queue.QueueConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeadlineReminderScheduler {

    private final LookupTaskRepository taskRepository;
    private final WorkerNotificationRepository notificationRepository;
    private final RedisProducer redisProducer;

    @Scheduled(fixedDelay = 60000)
    @Transactional(readOnly = true)
    public void scheduleReminders() {
        log.info("Worker Deadline Reminder Scheduler scanning for tasks...");
        List<LookupTaskEntity> tasks = taskRepository.findByStatusNot("COMPLETED");
        LocalDateTime now = LocalDateTime.now();

        for (LookupTaskEntity task : tasks) {
            if ("DONE".equals(task.getStatus())) {
                continue;
            }

            LocalDate deadlineDate = task.getDeadline();
            if (deadlineDate == null) {
                continue;
            }

            LocalDateTime deadlineTime = deadlineDate.atTime(17, 0, 0);
            Duration duration = Duration.between(now, deadlineTime);
            long hoursLeft = duration.toHours();

            if (task.getEmployees() == null || task.getEmployees().isEmpty()) {
                continue;
            }

            LookupUserEntity employee = task.getEmployees().get(0);
            LookupUserEntity manager = task.getProject() != null ? task.getProject().getManager() : null;

            if (duration.isNegative() || duration.isZero()) {
                enqueueReminder(task, employee, "OVERDUE", "Task Overdue Alert",
                        "Your task '" + task.getName() + "' is overdue! Deadline was " + deadlineDate);
                if (manager != null) {
                    enqueueReminder(task, manager, "OVERDUE_MANAGER", "Task Overdue Alert (Manager Copy)",
                            "The task '" + task.getName() + "' assigned to " + employee.getFullName() + " is overdue.");
                }
            } else {
                if (hoursLeft <= 1) {
                    enqueueReminder(task, employee, "REMINDER_1H", "Task Due in 1 Hour",
                            "Your task '" + task.getName() + "' is due in 1 hour!");
                } else if (hoursLeft <= 12) {
                    enqueueReminder(task, employee, "REMINDER_12H", "Task Due in 12 Hours",
                            "Your task '" + task.getName() + "' is due in 12 hours.");
                } else if (hoursLeft <= 24) {
                    enqueueReminder(task, employee, "REMINDER_24H", "Task Due in 24 Hours",
                            "Your task '" + task.getName() + "' is due in 24 hours.");
                } else if (hoursLeft <= 48) {
                    enqueueReminder(task, employee, "REMINDER_48H", "Task Due in 48 Hours",
                            "Your task '" + task.getName() + "' is due in 48 hours.");
                }
            }
        }
    }

    private void enqueueReminder(LookupTaskEntity task, LookupUserEntity user, String type, String title, String description) {
        if (notificationRepository.existsByUserIdAndTaskIdAndType(user.getId(), task.getId(), type)) {
            return;
        }

        Map<String, Object> job = new HashMap<>();
        job.put("taskId", task.getId());
        job.put("userId", user.getId());
        job.put("type", type);
        job.put("title", title);
        job.put("message", description);
        job.put("userEmail", user.getEmail());

        redisProducer.pushToQueue(QueueConstants.DEADLINE_REMINDERS_QUEUE, job);
    }
}
