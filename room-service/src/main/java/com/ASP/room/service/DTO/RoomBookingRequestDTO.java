package com.ASP.room.service.DTO;

import com.ASP.room.service.Entity.TimeSlot;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record RoomBookingRequestDTO(@NotNull LocalDate date,
                                    @NotNull TimeSlot slot,
                                    @Min(1) int capacity,
                                    boolean needsProjector,
                                    boolean needsAvSystem,
                                    @NotBlank String bookedBy) {
}
