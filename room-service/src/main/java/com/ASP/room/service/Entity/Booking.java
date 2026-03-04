package com.ASP.room.service.Entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;


@Entity
@Table(
        name = "bookings",
        uniqueConstraints = @UniqueConstraint(columnNames = {"room_id", "date", "slot"})
)
@Data
@NoArgsConstructor
@ToString(exclude = "room")
@EqualsAndHashCode(exclude = "room")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Back reference to the room – THIS is what caused the infinite nesting
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "room_id", nullable = false)
    @JsonBackReference
    private Room room;

    private LocalDate date;

    @Enumerated(EnumType.STRING)
    private TimeSlot slot;

    private String bookedBy;

    // What the requester originally needed (used for fallback / reassignment logic)
    private int requiredCapacity;
    private boolean needsProjector;
    private boolean needsAvSystem;

    @Enumerated(EnumType.STRING)
    private BookingStatus status = BookingStatus.CONFIRMED;

    /** True if we put them into a bigger room than requested (fallback). */
    private boolean fallbackAssignment = false;

    /** For "who booked first" ordering. */
    private LocalDateTime createdAt = LocalDateTime.now();

    /** Duration of the booking in hours (slot = 2h, +1h max extension). */
    private int durationHours = 2;

    /** Has this booking already been extended by 1 hour? */
    private boolean extendedOnce = false;

    public Room getRoom() {
        return room;
    }
}