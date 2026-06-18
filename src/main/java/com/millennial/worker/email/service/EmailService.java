package com.millennial.worker.email.service;

import com.millennial.worker.email.dto.EmailDto;
import com.millennial.worker.email.provider.EmailProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final EmailProvider emailProvider;

    /**
     * Send email with exponential backoff retry.
     */
    public void sendEmailWithRetry(EmailDto emailDto) {
        int maxAttempts = 3;
        int attempt = 0;
        long backoffMs = 2000;

        while (attempt < maxAttempts) {
            try {
                attempt++;
                log.info("Attempting to send email to {} (Attempt {}/{})", emailDto.getTo(), attempt, maxAttempts);
                emailProvider.send(emailDto);
                return;
            } catch (Exception e) {
                log.error("Failed to send email to {} on attempt {}/{}. Error: {}", 
                        emailDto.getTo(), attempt, maxAttempts, e.getMessage());
                if (attempt >= maxAttempts) {
                    log.error("Max retry attempts reached. Email to {} failed permanently.", emailDto.getTo());
                    break;
                }
                try {
                    log.info("Backing off for {} ms before next attempt...", backoffMs);
                    Thread.sleep(backoffMs);
                    backoffMs *= 2;
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.error("Retry backoff interrupted: {}", ie.getMessage());
                    break;
                }
            }
        }
    }

    /**
     * Process bulk email sending.
     */
    public void sendBulkEmails(List<EmailDto> emails) {
        log.info("Processing bulk emails. Total size: {}", emails.size());
        for (EmailDto email : emails) {
            sendEmailWithRetry(email);
        }
    }
}
