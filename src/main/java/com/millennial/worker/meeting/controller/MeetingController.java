package com.millennial.worker.meeting.controller;

import com.millennial.worker.email.dto.EmailDto;
import com.millennial.worker.email.service.EmailService;
import com.millennial.worker.meeting.entity.WorkerMeetingEntity;
import com.millennial.worker.meeting.repository.WorkerMeetingRepository;
import com.millennial.worker.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Map;

@RestController
@RequestMapping("/api/worker/meetings")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class MeetingController {

    private final WorkerMeetingRepository meetingRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;

    private LocalDateTime parseDateTime(String str) {
        if (str == null || str.trim().isEmpty()) return null;
        try {
            return LocalDateTime.parse(str, DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception e) {
            try {
                return java.time.OffsetDateTime.parse(str, DateTimeFormatter.ISO_DATE_TIME).toLocalDateTime();
            } catch (Exception ex) {
                return LocalDateTime.parse(str.replace("Z", "").substring(0, 19));
            }
        }
    }

    @PostMapping
    public WorkerMeetingEntity scheduleMeeting(@RequestBody Map<String, Object> body) {
        log.info("Scheduling meeting: {}", body);
        String title = (String) body.get("title");
        String meetLink = (String) body.get("meetLink");
        String meetingTimeStr = (String) body.get("meetingTime");
        Long projectId = Long.parseLong(body.get("projectId").toString());
        String recipientIds = (String) body.get("recipientIds");
        String recipientEmails = (String) body.get("recipientEmails");

        LocalDateTime meetingTime = parseDateTime(meetingTimeStr);
        LocalDateTime reminderTime = null;
        if (body.containsKey("reminderTime") && body.get("reminderTime") != null) {
            reminderTime = parseDateTime((String) body.get("reminderTime"));
        }

        WorkerMeetingEntity meeting = WorkerMeetingEntity.builder()
                .title(title)
                .meetLink(meetLink)
                .meetingTime(meetingTime)
                .projectId(projectId)
                .recipientIds(recipientIds)
                .recipientEmails(recipientEmails)
                .alerted1m(false)
                .reminderTime(reminderTime)
                .reminderAlerted(false)
                .build();

        WorkerMeetingEntity saved = meetingRepository.save(meeting);
        log.info("Meeting saved: {}", saved.getId());

        String formattedTime = meetingTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        String alertMessage = "New meeting scheduled: '" + title + "' on " + formattedTime + ". Link: " + meetLink;

        if (recipientIds != null && !recipientIds.isEmpty()) {
            Arrays.stream(recipientIds.split(","))
                    .map(String::trim)
                    .map(Long::parseLong)
                    .forEach(uId -> {
                        try {
                            notificationService.saveAndSendNotification(uId, null, "SYSTEM", "Meeting Scheduled", alertMessage);
                        } catch (Exception e) {
                            log.error("Failed to send SSE notification to user {}: {}", uId, e.getMessage());
                        }
                    });
        }

        if (recipientEmails != null && !recipientEmails.isEmpty()) {
            Arrays.stream(recipientEmails.split(","))
                    .map(String::trim)
                    .forEach(email -> {
                        try {
                            EmailDto emailDto = new EmailDto(
                                    email,
                                    "Meeting Scheduled: " + title,
                                    "<p>A meeting has been scheduled for your project:</p>" +
                                    "<p><strong>Topic:</strong> " + title + "</p>" +
                                    "<p><strong>Time:</strong> " + formattedTime + "</p>" +
                                    "<p><strong>Link:</strong> <a href=\"" + meetLink + "\">Join Meeting</a></p>"
                            );
                            emailService.sendEmailWithRetry(emailDto);
                        } catch (Exception e) {
                            log.error("Failed to send email to {}: {}", email, e.getMessage());
                        }
                    });
        }

        return saved;
    }
}
