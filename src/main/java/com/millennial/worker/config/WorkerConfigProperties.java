package com.millennial.worker.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.worker")
@Data
public class WorkerConfigProperties {

    private Redis redis = new Redis();
    private Smtp smtp = new Smtp();
    private Resend resend = new Resend();

    @Data
    public static class Redis {
        private String host = "localhost";
        private int port = 6379;
        private String password = "";
    }

    @Data
    public static class Smtp {
        private String host = "localhost";
        private int port = 587;
        private String username = "";
        private String password = "";
        private String from = "no-reply@millennial.com";
    }

    @Data
    public static class Resend {
        private String apiKey = "";
        private String from = "GetPlaced <noreply@getplaced.tech>";
    }
}
