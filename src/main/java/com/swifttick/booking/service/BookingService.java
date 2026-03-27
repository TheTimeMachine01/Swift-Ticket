package com.swifttick.booking.service;

import com.swifttick.booking.dto.BookingRequest;
import com.swifttick.booking.dto.BookingResponse;
import com.swifttick.booking.model.Order;
import com.swifttick.booking.model.OrderStatus;
import com.swifttick.booking.repository.OrderRepository;
import com.swifttick.event.model.Seat;
import com.swifttick.event.service.EventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class BookingService {

    private final EventService eventService;
    private final OrderRepository orderRepository;
    private final RedissonClient redissonClient;
    private final StringRedisTemplate redisTemplate;

    private static final String LOCK_PREFIX = "seat:lock:";
    private static final String STATUS_PREFIX = "seat:status:";

    @Transactional
    public BookingResponse createBooking(Long userId, BookingRequest request) {
        Long seatId = request.getSeatId();
        
        // 1. Fast-Fail Check (Redis)
        String redisStatus = redisTemplate.opsForValue().get(STATUS_PREFIX + seatId);
        if (redisStatus != null && !"AVAILABLE".equals(redisStatus)) {
            throw new RuntimeException("Seat is not available (Fast-Fail)");
        }

        // 2. Distributed Lock (Redisson)
        String lockKey = LOCK_PREFIX + seatId;
        RLock lock = redissonClient.getLock(lockKey);
        
        try {
            // Wait for 2 seconds, lease for 10 seconds
            if (!lock.tryLock(2, 10, TimeUnit.SECONDS)) {
                throw new RuntimeException("Could not acquire lock for seat");
            }

            // 3. Database Check & Update (with Optimistic Locking)
            Seat seat = eventService.getSeatById(seatId)
                    .orElseThrow(() -> new RuntimeException("Seat not found"));
            
            // This method in EventService will trigger JPA Optimistic Locking
            eventService.lockSeat(seatId);

            // 4. Update Redis Fast-Fail Status
            redisTemplate.opsForValue().set(STATUS_PREFIX + seatId, "LOCKED", 10, TimeUnit.MINUTES);

            // 5. Create Order
            Order order = Order.builder()
                    .userId(userId)
                    .seatId(seatId)
                    .amount(seat.getPrice())
                    .status(OrderStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .expiresAt(LocalDateTime.now().plusMinutes(10))
                    .build();

            orderRepository.save(order);

            return BookingResponse.builder()
                    .orderId(order.getId())
                    .status(order.getStatus())
                    .expiresAt(order.getExpiresAt())
                    .build();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while acquiring lock", e);
        } catch (Exception e) {
            log.error("Error during booking process for seat {}: {}", seatId, e.getMessage());
            throw e;
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
