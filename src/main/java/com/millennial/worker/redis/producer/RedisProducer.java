package com.millennial.worker.redis.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisProducer {

    private final RedisTemplate<String, Object> redisTemplate;

    public void pushToQueue(String queueName, Object payload) {
        try {
            redisTemplate.opsForList().rightPush(queueName, payload);
            log.info("Pushed job to Redis queue [{}]: {}", queueName, payload);
        } catch (Exception e) {
            log.error("Failed to push job to Redis queue [{}]: {}", queueName, e.getMessage(), e);
        }
    }
}
