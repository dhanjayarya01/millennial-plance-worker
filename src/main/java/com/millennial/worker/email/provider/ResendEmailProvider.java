package com.millennial.worker.email.provider;

import com.millennial.worker.config.WorkerConfigProperties;
import com.millennial.worker.email.dto.EmailDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ResendEmailProvider implements EmailProvider {

    private final WorkerConfigProperties config;
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public void send(EmailDto emailDto) throws Exception {
        String apiKey = config.getResend().getApiKey();
        String from = config.getResend().getFrom();

        log.info("Sending email via Resend to: {}, subject: {}", emailDto.getTo(), emailDto.getSubject());

        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException("Resend API Key is not configured!");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        Map<String, Object> body = Map.of(
                "from", from,
                "to", emailDto.getTo(),
                "subject", emailDto.getSubject(),
                "html", emailDto.getHtml()
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        String url = "https://api.resend.com/emails";

        try {
            Map<?, ?> response = restTemplate.postForObject(url, request, Map.class);
            log.info("Resend email sent successfully. Response: {}", response);
        } catch (Exception e) {
            log.error("Failed to send email via Resend: {}", e.getMessage());
            throw e;
        }
    }
}
