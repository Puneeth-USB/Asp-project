package com.ASP.request.service.Entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "maintenance_requests")
@NoArgsConstructor
public class MaintenanceRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Room id from room-service */
    @Column(nullable = false)
    private Long roomId;

    /** Name of the person who booked / raised the request */
    @Column(nullable = false, length = 100)
    private String bookedBy;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "scope",
            nullable = false,
            length = 32,
            columnDefinition = "ENUM('FULL_ROOM', 'PROJECTOR', 'AV_SYSTEM')"
    )
    private FacilityScope scope;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "priority",
            nullable = false,
            length = 16,
            columnDefinition = "ENUM('LOW', 'HIGH')"
    )
    private PriorityLevel priority;

    @Column(nullable = false, length = 500)
    private String description;

    /**
     * At first: null (unassigned).
     * Later: admin sets this to a valid MaintenanceStaff.id.
     */
    @Column
    private Long assignedStaffId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MaintenanceStatus status = MaintenanceStatus.OPEN;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime completedAt;
}
