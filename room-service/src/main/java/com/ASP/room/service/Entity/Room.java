package com.ASP.room.service.Entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
@ToString(exclude = "bookings")
@EqualsAndHashCode(exclude = "bookings")
@SequenceGenerator(
        name = "room_seq",
        sequenceName = "room_seq",
        allocationSize = 1
)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "room_seq")
    private Long id;  // room number

    private int capacity;

    // Facilities actually present in the room
    private boolean hasProjector;
    private boolean hasAvSystem;

    // Whether those facilities are currently available (maintenance can flip these)
    private boolean projectorAvailable = true;
    private boolean avSystemAvailable = true;

    @Enumerated(EnumType.STRING)
    private RoomStatus status = RoomStatus.AVAILABLE;

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference   // pairs with @JsonBackReference on Booking.room
    private List<Booking> bookings = new ArrayList<>();

    @PrePersist
    public void initAvailability() {
        if (!hasProjector) {
            projectorAvailable = false;
        }
        if (!hasAvSystem) {
            avSystemAvailable = false;
        }
    }
}
