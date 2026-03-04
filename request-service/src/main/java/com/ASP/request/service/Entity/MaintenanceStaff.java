package com.ASP.request.service.Entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Set;
@Entity
@Data
public class MaintenanceStaff {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "staff_expertise", joinColumns = @JoinColumn(name = "staff_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "expertise", nullable = false)
    private Set<ExpertiseType> expertises;

    /**
     * Number of CURRENTLY assigned maintenance requests.
     * Starts at 0.
     */
    @Column(nullable = false)
    private int assignedRequestCount = 0;
}
