package com.swifttick.booking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@Slf4j
@RequiredArgsConstructor
public class WaitlistService {

    private final StringRedisTemplate redisTemplate;
    private static final String WAITLIST_PREFIX = "waitlist:";

    public void addToWaitlist(Long eventId, Long seatId, Long userId) {
        String key = WAITLIST_PREFIX + eventId + ":" + seatId;
        double score = Instant.now().toEpochMilli();
        redisTemplate.opsForZSet().add(key, userId.toString(), score);
        log.info("User {} added to waitlist for seat {}", userId, seatId);
    }

    public String popFromWaitlist(Long eventId, Long seatId) {
        String key = WAITLIST_PREFIX + eventId + ":" + seatId;
        return redisTemplate.opsForZSet().popMin(key).getValue();
    }
}
