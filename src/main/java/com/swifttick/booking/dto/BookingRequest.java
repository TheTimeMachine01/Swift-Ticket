package com.swifttick.booking.dto;

import lombok.Data;

@Data
public class BookingRequest {
    private Long eventId;
    private Long seatId;
}
