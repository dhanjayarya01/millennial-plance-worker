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
        LocalDateTime targetTime = now.plusMinutes(1).plusSeconds(30);
        List<WorkerMeetingEntity> meetings = meetingRepository.findByMeetingTimeBeforeAndAlerted1mFalse(targetTime);

        for (WorkerMeetingEntity meeting : meetings) {
            Duration duration = Duration.between(now, meeting.getMeetingTime());
            long secondsLeft = duration.toSeconds();

            if (secondsLeft <= 90) {
                log.info("Meeting '{}' starting in {} seconds. Triggering 1-minute alert.", meeting.getTitle(), secondsLeft);

                String alertMessage = "Meeting starting in 1 minute: '" + meeting.getTitle() + "'! Join now: " + meeting.getMeetLink();

                String recipientIds = meeting.getRecipientIds();
                if (recipientIds != null && !recipientIds.isEmpty()) {
                    Arrays.stream(recipientIds.split(","))
                            .map(String::trim)
                            .map(Long::parseLong)
                            .forEach(uId -> {
                                try {
                                    notificationService.saveAndSendNotification(uId, null, "SYSTEM", "Meeting Starting Soon", alertMessage);
                                } catch (Exception e) {
                                    log.error("Failed to send 1-min SSE notification to user {}: {}", uId, e.getMessage());
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
                                            "Meeting Starting Soon: " + meeting.getTitle(),
                                            "<p>Your meeting is starting in 1 minute!</p>" +
                                            "<p><strong>Topic:</strong> " + meeting.getTitle() + "</p>" +
                                            "<p><strong>Join Link:</strong> <a href=\"" + meeting.getMeetLink() + "\">Join Now</a></p>"
                                    );
                                    emailService.sendEmailWithRetry(emailDto);
                                } catch (Exception e) {
                                    log.error("Failed to send 1-min email to {}: {}", email, e.getMessage());
                                }
                            });
                }

                meeting.setAlerted1m(true);
                meetingRepository.save(meeting);
            }
        }
    }
}
