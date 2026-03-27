package com.swifttick.booking.controller;

import com.swifttick.booking.service.WaitlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class BookingWaitlistController {

    private final WaitlistService waitlistService;

    @PostMapping("/{eventId}/seats/{seatId}/waitlist")
    public ResponseEntity<String> joinWaitlist(
            @RequestHeader("X-User-ID") Long userId,
            @PathVariable Long eventId,
            @PathVariable Long seatId) {
        
        waitlistService.addToWaitlist(eventId, seatId, userId);
        return ResponseEntity.ok("Joined waitlist for seat " + seatId);
    }
}
