package com.millennial.worker.email.provider;

import com.millennial.worker.email.dto.EmailDto;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SmtpEmailProvider implements EmailProvider {
    @Override
    public void send(EmailDto emailDto) throws Exception {
        log.warn("SMTP email provider is disabled. Use ResendEmailProvider instead.");
    }
}
