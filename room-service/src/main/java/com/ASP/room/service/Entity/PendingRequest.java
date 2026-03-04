package com.ASP.room.service.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "pending_requests")
public class PendingRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate date;

    @Enumerated(EnumType.STRING)
    private TimeSlot slot;

    private int requiredCapacity;

    private boolean needsProjector;
    private boolean needsAvSystem;

    private String requestedBy;

    private Instant createdAt = Instant.now();
}
