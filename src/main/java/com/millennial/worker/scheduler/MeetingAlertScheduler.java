package com.millennial.worker.scheduler;

import com.millennial.worker.email.dto.EmailDto;
import com.millennial.worker.email.service.EmailService;
import com.millennial.worker.meeting.entity.WorkerMeetingEntity;
import com.millennial.worker.meeting.repository.WorkerMeetingRepository;
import com.millennial.worker.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class MeetingAlertScheduler {

    private final WorkerMeetingRepository meetingRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;

    @Scheduled(fixedDelay = 30000)
    @Transactional
    public void checkMeetingAlerts() {
        LocalDateTime now = LocalDateTime.now();

        // 1. Process custom reminders
        List<WorkerMeetingEntity> customMeetings = meetingRepository.findByReminderTimeBeforeAndReminderAlertedFalse(now);
        for (WorkerMeetingEntity meeting : customMeetings) {
            log.info("Meeting '{}' custom reminder time reached. Triggering alert.", meeting.getTitle());

            String formattedTime = meeting.getMeetingTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            Duration duration = Duration.between(now, meeting.getMeetingTime());
            long secondsLeft = Math.max(0, duration.toSeconds());
            long mins = secondsLeft / 60;
            long secs = secondsLeft % 60;
            String timeText = mins > 0 ? (mins + "m " + secs + "s") : (secs + "s");

            String alertMessage = "Reminder: Meeting '" + meeting.getTitle() + "' is starting in " + timeText + "! Join here: " + meeting.getMeetLink();
            sendMeetingAlert(meeting, alertMessage);

            meeting.setReminderAlerted(true);
            meetingRepository.save(meeting);
        }

        // 2. Process legacy fallback (for meetings without a custom reminderTime set)
        LocalDateTime legacyTarget = now.plusMinutes(1).plusSeconds(30);
        List<WorkerMeetingEntity> legacyMeetings = meetingRepository.findByMeetingTimeBeforeAndAlerted1mFalse(legacyTarget);
        for (WorkerMeetingEntity meeting : legacyMeetings) {
            if (meeting.getReminderTime() == null) {
                Duration duration = Duration.between(now, meeting.getMeetingTime());
                long secondsLeft = duration.toSeconds();
                if (secondsLeft <= 90) {
                    log.info("Meeting '{}' starting in {} seconds. Legacy 1-min alert.", meeting.getTitle(), secondsLeft);

                    String alertMessage = "Meeting starting in 1 minute: '" + meeting.getTitle() + "'! Join now: " + meeting.getMeetLink();
                    sendMeetingAlert(meeting, alertMessage);
                }
            }
            meeting.setAlerted1m(true);
            meetingRepository.save(meeting);
        }
    }

    private void sendMeetingAlert(WorkerMeetingEntity meeting, String alertMessage) {
        String title = meeting.getTitle();
        String meetLink = meeting.getMeetLink();

        String recipientIds = meeting.getRecipientIds();
        if (recipientIds != null && !recipientIds.isEmpty()) {
            Arrays.stream(recipientIds.split(","))
                    .map(String::trim)
                    .map(Long::parseLong)
                    .forEach(uId -> {
                        try {
                            notificationService.saveAndSendNotification(uId, null, "SYSTEM", "Meeting Reminder", alertMessage);
                        } catch (Exception e) {
                            log.error("Failed to send SSE notification to user {}: {}", uId, e.getMessage());
                        }
                    });
        }

        String recipientEmails = meeting.getRecipientEmails();
        if (recipientEmails != null && !recipientEmails.isEmpty()) {
            Arrays.stream(recipientEmails.split(","))
                    .map(String::trim)
                    .forEach(email -> {
                        try {
                            EmailDto emailDto = new EmailDto(
                                    email,
                                    "Meeting Reminder: " + title,
                                    "<p>Reminder for your upcoming meeting:</p>" +
                                    "<p><strong>Topic:</strong> " + title + "</p>" +
                                    "<p><strong>Alert:</strong> " + alertMessage + "</p>" +
                                    "<p><strong>Join Link:</strong> <a href=\"" + meetLink + "\">Join Meeting</a></p>"
                            );
                            emailService.sendEmailWithRetry(emailDto);
                        } catch (Exception e) {
                            log.error("Failed to send email to {}: {}", email, e.getMessage());
                        }
                    });
        }
    }
}
