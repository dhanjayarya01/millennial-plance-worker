package com.millennial.worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@SpringBootApplication
@EnableScheduling
public class WorkerApplication {

    public static void main(String[] args) {
        loadEnvFile();
        SpringApplication.run(WorkerApplication.class, args);
    }

    private static void loadEnvFile() {
        Path envPath = Paths.get(".env");
        if (!Files.exists(envPath)) {
            // Also try looking in current directory
            envPath = Paths.get("../millennial-plance-worker/.env");
        }
        if (!Files.exists(envPath)) {
            // Try parent directory
            envPath = Paths.get("../.env");
        }
        if (!Files.exists(envPath)) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(envPath.toFile()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                int idx = line.indexOf('=');
                if (idx < 0) continue;
                String key = line.substring(0, idx).trim();
                String value = line.substring(idx + 1).trim();
                if (System.getenv(key) == null && System.getProperty(key) == null) {
                    System.setProperty(key, value);
                }
            }
        } catch (IOException e) {
            System.err.println("[WorkerApplication] Warning: Could not load .env file - " + e.getMessage());
        }
    }
}
