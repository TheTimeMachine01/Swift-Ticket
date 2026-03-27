package com.swifttick.booking.dto;

import com.swifttick.booking.model.OrderStatus;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class BookingResponse {
    private UUID orderId;
    private OrderStatus status;
    private LocalDateTime expiresAt;
}
