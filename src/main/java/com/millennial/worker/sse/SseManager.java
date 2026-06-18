package com.millennial.worker.sse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class SseManager {

    private final Map<Long, List<SseEmitter>> userEmitters = new ConcurrentHashMap<>();
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    public SseEmitter createEmitter(Long userId) {
        SseEmitter emitter = new SseEmitter(120000L); // 2 minutes timeout
        
        userEmitters.computeIfAbsent(userId, k -> new ArrayList<>()).add(emitter);
        log.info("Created SSE emitter for userId: {}. Current active emitters: {}", userId, userEmitters.get(userId).size());

        emitter.onCompletion(() -> removeEmitter(userId, emitter));
        emitter.onTimeout(() -> removeEmitter(userId, emitter));
        emitter.onError((e) -> removeEmitter(userId, emitter));

        // Send a heartbeat init message
        try {
            emitter.send(SseEmitter.event().name("init").data("Connected successfully"));
        } catch (IOException e) {
            log.error("Failed to send init event for user: {}", userId, e);
            removeEmitter(userId, emitter);
        }

        return emitter;
    }

    public void sendNotification(Long userId, Object notification) {
        List<SseEmitter> emitters = userEmitters.get(userId);
        if (emitters == null || emitters.isEmpty()) {
            log.debug("No active SSE emitters found for userId: {}", userId);
            return;
        }

        String jsonString;
        try {
            jsonString = objectMapper.writeValueAsString(notification);
        } catch (Exception e) {
            log.error("Failed to serialize notification for SSE to JSON: {}", e.getMessage());
            return;
        }

        List<SseEmitter> deadEmitters = new ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("notification").data(jsonString));
                log.info("Dispatched SSE notification to userId: {}", userId);
            } catch (Exception e) {
                log.warn("Failed to dispatch SSE event, marking emitter as dead: {}", e.getMessage());
                deadEmitters.add(emitter);
            }
        }

        if (!deadEmitters.isEmpty()) {
            emitters.removeAll(deadEmitters);
            log.info("Cleaned up {} dead emitters for userId: {}", deadEmitters.size(), userId);
        }
    }

    private void removeEmitter(Long userId, SseEmitter emitter) {
        List<SseEmitter> emitters = userEmitters.get(userId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                userEmitters.remove(userId);
            }
            log.info("Removed SSE emitter for userId: {}", userId);
        }
    }
}
