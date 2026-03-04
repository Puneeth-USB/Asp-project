package com.ASP.room.service.Repository;

import com.ASP.room.service.Entity.Booking;
import com.ASP.room.service.Entity.BookingStatus;
import com.ASP.room.service.Entity.Room;
import com.ASP.room.service.Entity.TimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepo extends JpaRepository<Booking, Long> {


    Optional<Booking> findByRoomAndDateAndSlotAndStatus(
            Room room, LocalDate date, TimeSlot slot, BookingStatus status);
    List<Booking> findByDateAndSlotAndStatusAndFallbackAssignmentTrueOrderByCreatedAtAsc(
            LocalDate date, TimeSlot slot, BookingStatus status
    );
    List<Booking> findByFallbackAssignmentTrueOrderByCreatedAtAsc();

    List<Booking> findByBookedByAndStatus(String bookedBy, BookingStatus status);

    List<Booking> findByRoomAndStatus(Room room, BookingStatus bookingStatus);
    // ➕ add this:
    Optional<Booking> findByRoomAndDateAndSlot(
            Room room, LocalDate date, TimeSlot slot);
}

