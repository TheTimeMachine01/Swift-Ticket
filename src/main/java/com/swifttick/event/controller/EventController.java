package com.swifttick.event.controller;

import com.swifttick.event.model.Seat;
import com.swifttick.event.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class EventController {

    private final SeatRepository seatRepository;

    @GetMapping("/{eventId}/seats")
    public ResponseEntity<List<SeatResponse>> getSeats(@PathVariable Long eventId) {
        List<Seat> seats = seatRepository.findByEventId(eventId);
        List<SeatResponse> response = seats.stream()
                .map(s -> new SeatResponse(s.getId(), s.getSeatNumber(), s.getPrice(), s.getStatus().name()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    public record SeatResponse(Long seatId, String seatNumber, java.math.BigDecimal price, String status) {}
}
