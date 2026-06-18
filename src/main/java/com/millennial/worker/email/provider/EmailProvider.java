package com.millennial.worker.email.provider;

import com.millennial.worker.email.dto.EmailDto;

public interface EmailProvider {
    void send(EmailDto emailDto) throws Exception;
}
