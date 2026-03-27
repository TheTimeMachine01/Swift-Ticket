package com.swifttick.event.service;

import com.swifttick.event.model.Seat;
import com.swifttick.event.model.SeatStatus;
import com.swifttick.event.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EventService {

    private final SeatRepository seatRepository;

    public Optional<Seat> getSeatById(Long seatId) {
        return seatRepository.findById(seatId);
    }

    @Transactional
    public void lockSeat(Long seatId) {
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new RuntimeException("Seat not found"));
        
        if (seat.getStatus() != SeatStatus.AVAILABLE) {
            throw new IllegalStateException("Seat is not available");
        }
        
        seat.setStatus(SeatStatus.LOCKED);
        seatRepository.save(seat);
    }

    @Transactional
    public void releaseSeat(Long seatId) {
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new RuntimeException("Seat not found"));
        
        seat.setStatus(SeatStatus.AVAILABLE);
        seatRepository.save(seat);
    }

    @Transactional
    public void bookSeat(Long seatId) {
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new RuntimeException("Seat not found"));
        
        seat.setStatus(SeatStatus.BOOKED);
        seatRepository.save(seat);
    }
}
