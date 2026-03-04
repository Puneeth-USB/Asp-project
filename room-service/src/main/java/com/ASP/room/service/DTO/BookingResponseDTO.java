package com.ASP.room.service.DTO;

import com.ASP.room.service.Entity.BookingStatus;

public record BookingResponseDTO(Long bookingId,
                                 Long roomId,
                                 BookingStatus status,
                                 String message) {
}
