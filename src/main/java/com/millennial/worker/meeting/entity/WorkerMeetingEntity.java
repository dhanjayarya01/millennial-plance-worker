package com.millennial.worker.meeting.entity;

import lombok.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "meetings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkerMeetingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(name = "meet_link", nullable = false)
    private String meetLink;

    @Column(name = "meeting_time", nullable = false)
    private LocalDateTime meetingTime;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "recipient_ids", length = 1000)
    private String recipientIds; // Comma-separated user IDs

    @Column(name = "recipient_emails", length = 2000)
    private String recipientEmails; // Comma-separated emails

    @Column(name = "alerted_1m", nullable = false)
    private boolean alerted1m;
}
