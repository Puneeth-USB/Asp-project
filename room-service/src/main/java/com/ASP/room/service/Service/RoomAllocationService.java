package com.ASP.room.service.Service;

import com.ASP.room.service.Client.NotificationClient;
import com.ASP.room.service.DTO.BookingResponseDTO;
import com.ASP.room.service.DTO.RoomBookingRequestDTO;
import com.ASP.room.service.Entity.*;
import com.ASP.room.service.Repository.BookingRepo;
import com.ASP.room.service.Repository.PendingRequestsRepo;
import com.ASP.room.service.Repository.RoomRepo;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
@RequiredArgsConstructor
public class RoomAllocationService {

    private final RoomRepo roomRepository;
    private final BookingRepo bookingRepository;
    private final PendingRequestsRepo pendingRequestRepository;
    private final NotificationClient notificationClient;


    // ================= NEW BOOKINGS ====================

    @Transactional
    public BookingResponseDTO bookRoom(RoomBookingRequestDTO req) {
        BookingResponseDTO response = tryAllocateWithoutAddingToWaitingList(req);
        if (response.bookingId() != null) {
            return response;
        }

        // No room available -> waiting list (first come first serve)
        PendingRequest pending = new PendingRequest();
        pending.setDate(req.date());
        pending.setSlot(req.slot());
        pending.setRequiredCapacity(req.capacity());
        pending.setNeedsProjector(req.needsProjector());
        pending.setNeedsAvSystem(req.needsAvSystem());
        pending.setRequestedBy(req.bookedBy());
        pendingRequestRepository.save(pending);

        return new BookingResponseDTO(
                null, null, null,
                "No room available. Added to waiting list (first-come-first-serve).");
    }

    @Transactional
    public BookingResponseDTO tryAllocateWithoutAddingToWaitingList(RoomBookingRequestDTO req) {
        List<Room> allRooms = roomRepository.findAll();

        List<Room> candidates = allRooms.stream()
                .filter(r -> r.getCapacity() >= req.capacity())
                .filter(r -> r.getStatus() != RoomStatus.UNDER_MAINTENANCE)
                .filter(r -> !req.needsProjector() || (r.isHasProjector() && r.isProjectorAvailable()))
                .filter(r -> !req.needsAvSystem() || (r.isHasAvSystem() && r.isAvSystemAvailable()))
                .sorted(Comparator.comparingInt(Room::getCapacity)) // smallest that fits
                .toList();

        for (Room room : candidates) {

            boolean hasConfirmedBooking = bookingRepository
                    .findByRoomAndDateAndSlotAndStatus(
                            room, req.date(), req.slot(), BookingStatus.CONFIRMED)
                    .isPresent();

            if (!hasConfirmedBooking) {
                Booking booking = bookingRepository
                        .findByRoomAndDateAndSlot(room, req.date(), req.slot())
                        .orElseGet(Booking::new);

                booking.setRoom(room);
                booking.setDate(req.date());
                booking.setSlot(req.slot());
                booking.setBookedBy(req.bookedBy());
                booking.setRequiredCapacity(req.capacity());
                booking.setNeedsProjector(req.needsProjector());
                booking.setNeedsAvSystem(req.needsAvSystem());
                booking.setStatus(BookingStatus.CONFIRMED);
                booking.setDurationHours(2);
                booking.setExtendedOnce(false);

                boolean isFallback = room.getCapacity() > req.capacity();
                booking.setFallbackAssignment(isFallback);

                bookingRepository.save(booking);

                // Mark room as BOOKED
                room.setStatus(RoomStatus.BOOKED);
                roomRepository.save(room);

                // Send notification to user
                String subject = "Room Booking Confirmed";
                String bookingDetails = " Room ID: " + room.getId() + ", Date: " + req.date() + ", Slot: " + req.slot();
                String body = (isFallback
                        ? "Your room booking is confirmed with a fallback room (more capacity). "
                        : "Your room booking is confirmed with an optimal room. ") + bookingDetails;
                boolean emailSent = Boolean.TRUE.equals(notificationClient.sendEmailByName(req.bookedBy(), subject, body).block());
                String emailStatus = emailSent ? " Email sent." : " Email sending failed.";

                String allocationMessage = (isFallback
                        ? "Room allocated (fallback with more capacity)."
                        : "Room allocated with optimal capacity.");

                return new BookingResponseDTO(
                        booking.getId(),
                        room.getId(),  // id IS room number
                        booking.getStatus(),
                        allocationMessage + emailStatus
                );
            }
        }

        // No free room for that date/slot
        return new BookingResponseDTO(
                null, null, null,
                "No suitable room currently free"
        );
    }
    // ================= BOOKING EXTENSION (+1 hour, only once) ====================

    @Transactional
    public BookingResponseDTO extendBookingOneHour(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            return new BookingResponseDTO(
                    booking.getId(),
                    booking.getRoom().getId(),
                    booking.getStatus(),
                    "Only CONFIRMED bookings can be extended.");
        }

        if (booking.isExtendedOnce() || booking.getDurationHours() >= 3) {
            return new BookingResponseDTO(
                    booking.getId(),
                    booking.getRoom().getId(),
                    booking.getStatus(),
                    "Booking already extended once (maximum 1 hour extension).");
        }

        booking.setDurationHours(booking.getDurationHours() + 1); // +1 hour
        booking.setExtendedOnce(true);
        bookingRepository.save(booking);

        // Send notification to user
        String subject = "Room Booking Extended";
        String bookingDetails = " Room ID: " + booking.getRoom().getId() + ", Date: " + booking.getDate() + ", Slot: " + booking.getSlot();
        String body = "Your room booking has been extended by 1 hour. " + bookingDetails;
        boolean emailSent = Boolean.TRUE.equals(notificationClient.sendEmailByName(booking.getBookedBy(), subject, body).block());
        String emailStatus = emailSent ? " Email sent." : " Email sending failed.";

        return new BookingResponseDTO(
                booking.getId(),
                booking.getRoom().getId(),
                booking.getStatus(),
                "Booking extended by 1 hour (total " + booking.getDurationHours() + " hours). " + emailStatus
        );
    }

    // ================= MAINTENANCE HOOKS ====================

    @Transactional
    public void blockRoomOrFacility(Long roomId, FacilityScope scope) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));

        // --- Update room status/facility availability BEFORE releasing bookings ---
        boolean projectorWillBeUnavailable = scope == FacilityScope.PROJECTOR || !room.isProjectorAvailable();
        boolean avSystemWillBeUnavailable = scope == FacilityScope.AV_SYSTEM || !room.isAvSystemAvailable();

        switch (scope) {
            case FULL_ROOM -> room.setStatus(RoomStatus.UNDER_MAINTENANCE);
            case PROJECTOR -> {
                room.setProjectorAvailable(false);
                if (avSystemWillBeUnavailable) {
                    room.setStatus(RoomStatus.UNDER_MAINTENANCE);
                }
            }
            case AV_SYSTEM -> {
                room.setAvSystemAvailable(false);
                if (projectorWillBeUnavailable) {
                    room.setStatus(RoomStatus.UNDER_MAINTENANCE);
                }
            }
        }
        roomRepository.save(room);

        // For each active booking, release only if the booking requires the facility being blocked (or full room)
        List<Booking> activeBookings = bookingRepository.findByRoomAndStatus(room, BookingStatus.CONFIRMED);
        for (Booking booking : activeBookings) {
            boolean shouldRelease = false;
            switch (scope) {
                case FULL_ROOM -> shouldRelease = true;
                case PROJECTOR -> shouldRelease = booking.isNeedsProjector();
                case AV_SYSTEM -> shouldRelease = booking.isNeedsAvSystem();
            }
            if (shouldRelease) {
                // Release booking: will try fallback, then waiting list, but now room state is correct
                releaseRoomAndAssignIfPossible(booking.getId());
            }
        }
    }

    @Transactional
    public BookingResponseDTO unblockRoomOrFacility(Long roomId, FacilityScope scope) {
        if (scope == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Facility scope must be provided");
        }
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));

        switch (scope) {
            case FULL_ROOM -> room.setStatus(RoomStatus.AVAILABLE);
            case PROJECTOR -> {
                room.setProjectorAvailable(true);
                if (room.getStatus() == RoomStatus.UNDER_MAINTENANCE &&
                        (room.isProjectorAvailable() || room.isAvSystemAvailable())) {
                    room.setStatus(RoomStatus.AVAILABLE);
                }
            }
            case AV_SYSTEM -> {
                room.setAvSystemAvailable(true);
                if (room.getStatus() == RoomStatus.UNDER_MAINTENANCE &&
                        (room.isProjectorAvailable() || room.isAvSystemAvailable())) {
                    room.setStatus(RoomStatus.AVAILABLE);
                }
            }
        }

        roomRepository.save(room);
        Booking movedFallback = reassignFallbackBookingsForRoom(room);
        if (movedFallback != null) {
            return new BookingResponseDTO(
                movedFallback.getId(),
                room.getId(),
                movedFallback.getStatus(),
                "Room maintenance unblocked. Fallback booking reassigned (id=" + movedFallback.getId()
                    + ") for " + movedFallback.getBookedBy() + "."
            );
        }
        processWaitingList();
        return new BookingResponseDTO(
            null,
            room.getId(),
            null,
            "Room maintenance unblocked. No fallback reassignment. Waiting list processed."
        );
    }

    // ================= WAITING LIST ====================

    @Transactional
    public void processWaitingList() {
        List<PendingRequest> allPending = pendingRequestRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(PendingRequest::getCreatedAt)) // first-come-first-serve
                .toList();

        for (PendingRequest pr : allPending) {
            RoomBookingRequestDTO dto = new RoomBookingRequestDTO(
                    pr.getDate(),
                    pr.getSlot(),
                    pr.getRequiredCapacity(),
                    pr.isNeedsProjector(),
                    pr.isNeedsAvSystem(),
                    pr.getRequestedBy()
            );
            BookingResponseDTO res = tryAllocateWithoutAddingToWaitingList(dto);
            if (res.bookingId() != null) {
                pendingRequestRepository.delete(pr);
            }
        }
    }

    @Transactional
    public Booking reassignFallbackBookingsForRoom(Room targetRoom) {
        // When a (usually smaller/better-fit) room becomes AVAILABLE (e.g. maintenance completed),
        // move the earliest "fallback" booking that can fit into this room for its SAME date+slot.
        // Only move if it is an *improvement* (target room is smaller than the current room).
        List<Booking> fallbackBookings = bookingRepository.findByFallbackAssignmentTrueOrderByCreatedAtAsc();

        for (Booking booking : fallbackBookings) {
            Room oldRoom = booking.getRoom();
            if(booking.getStatus()==BookingStatus.CANCELLED){
                continue;
            }
            if (oldRoom != null && targetRoom.getCapacity() >= oldRoom.getCapacity()) {
                continue;
            }

            // Check if this booking can use the target room at its date+slot
            if (booking.getRequiredCapacity() > targetRoom.getCapacity()) {
                continue; // too big
            }

            if (targetRoom.getStatus() == RoomStatus.UNDER_MAINTENANCE) {
                continue;
            }
            if (booking.isNeedsProjector() &&
                    !(targetRoom.isHasProjector() && targetRoom.isProjectorAvailable())) {
                continue;
            }
            if (booking.isNeedsAvSystem() &&
                    !(targetRoom.isHasAvSystem() && targetRoom.isAvSystemAvailable())) {
                continue;
            }

            // Is the target room already booked for this date+slot?
            boolean slotTaken = bookingRepository
                    .findByRoomAndDateAndSlotAndStatus(
                            targetRoom, booking.getDate(), booking.getSlot(), BookingStatus.CONFIRMED)
                    .isPresent();
            if (slotTaken) {
                continue;
            }

            // Cancel the old booking for the old room
            booking.setStatus(BookingStatus.CANCELLED);
            bookingRepository.save(booking);

            // Create a new booking for the target room
            Booking newBooking = bookingRepository
                    .findByRoomAndDateAndSlot(targetRoom, booking.getDate(), booking.getSlot())
                    .orElseGet(Booking::new);
            newBooking.setRoom(targetRoom);
            newBooking.setDate(booking.getDate());
            newBooking.setSlot(booking.getSlot());
            newBooking.setBookedBy(booking.getBookedBy());
            newBooking.setRequiredCapacity(booking.getRequiredCapacity());
            newBooking.setNeedsProjector(booking.isNeedsProjector());
            newBooking.setNeedsAvSystem(booking.isNeedsAvSystem());
            newBooking.setStatus(BookingStatus.CONFIRMED);
            newBooking.setDurationHours(booking.getDurationHours());
            newBooking.setExtendedOnce(booking.isExtendedOnce());
            newBooking.setFallbackAssignment(targetRoom.getCapacity() > booking.getRequiredCapacity());
            newBooking.setCreatedAt(java.time.LocalDateTime.now());
            bookingRepository.save(newBooking);

            targetRoom.setStatus(RoomStatus.BOOKED);
            roomRepository.save(targetRoom);

            if (oldRoom != null) {
                oldRoom.setStatus(RoomStatus.AVAILABLE);
                roomRepository.save(oldRoom);
            }

            // One room can host only one booking per (date, slot)
            // Send notification to user of the new booking
            String subject = "Room Booking Reassigned";
            String bookingDetails = " Room ID: " + newBooking.getRoom().getId() + ", Date: " + newBooking.getDate() + ", Slot: " + newBooking.getSlot();
            String body = "Your booking has been reassigned to a new room. " + bookingDetails;
            boolean emailSent = Boolean.TRUE.equals(notificationClient.sendEmailByName(newBooking.getBookedBy(), subject, body).block());
            String emailStatus = emailSent ? " Email sent." : " Email sending failed.";
            // Optionally, you could log or use emailStatus in the response
            return newBooking;
        }
        return null;
    }

    @Transactional
    public List<Room> getCurrentlyBookedRoomsFor(String bookedBy) {
        List<Booking> bookings =
                bookingRepository.findByBookedByAndStatus(bookedBy, BookingStatus.CONFIRMED);

        return bookings.stream()
                .map(Booking::getRoom)
                .distinct()
                .toList();
    }

    @Transactional
    public List<PendingRequest> getPendingRequestsForName(String name) {
        return pendingRequestRepository.findByRequestedByOrderByCreatedAtAsc(name);
    }


    @Transactional
    public BookingResponseDTO releaseRoomAndAssignIfPossible(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No booking found with id: " + bookingId));

        Room room = booking.getRoom();

        // If not CONFIRMED anymore, nothing to do – just return info
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            return new BookingResponseDTO(
                    booking.getId(),
                    room.getId(),
                    booking.getStatus(),
                    "Booking is not active; no reassignment performed."
            );
        }

        LocalDate date = booking.getDate();
        TimeSlot slot = booking.getSlot();

        // Cancel this booking (the user explicitly released it)
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        // Send notification to user who released the room
        String subject = "Room Booking Released";
        String bookingDetails = " Room ID: " + room.getId() + ", Date: " + date + ", Slot: " + slot;
        String body = "Your booking has been released and the room is now available. " + bookingDetails;
        boolean emailSent = Boolean.TRUE.equals(notificationClient.sendEmailByName(booking.getBookedBy(), subject, body).block());
        String emailStatus = emailSent ? " Email sent." : " Email sending failed.";

        // Room becomes AVAILABLE only if not under maintenance
        if (room.getStatus() != RoomStatus.UNDER_MAINTENANCE) {
            room.setStatus(RoomStatus.AVAILABLE);
            roomRepository.save(room);
        }

        // Try to reassign to fallback booking
        Booking movedFallback = tryMoveEarliestFallbackBookingToRoom(room, date, slot);
        if (movedFallback != null) {
            // Only return the response, notification is sent in tryMoveEarliestFallbackBookingToRoom
            String fallbackDetails = " Room ID: " + movedFallback.getRoom().getId() + ", Date: " + movedFallback.getDate() + ", Slot: " + movedFallback.getSlot();
            return new BookingResponseDTO(
                    booking.getId(),
                    room.getId(),
                    booking.getStatus(),
                    "Booking cancelled. Freed room reassigned to fallback booking (id=" + movedFallback.getId()
                            + ") for " + movedFallback.getBookedBy() + "." + fallbackDetails
            );
        }

        // If not allocated to fallback, process waiting list
        processWaitingList();

        String message = room.getStatus() == RoomStatus.UNDER_MAINTENANCE
                ? "Booking cancelled; room is under maintenance."
                : "Booking cancelled; room is now AVAILABLE or assigned to waiting list.";
        return new BookingResponseDTO(
                booking.getId(),
                room.getId(),
                booking.getStatus(),
                message + bookingDetails + emailStatus
        );
    }



    /**
     * When a smaller/better-fit room becomes available for a specific (date, slot),
     * move the earliest fallback booking (CONFIRMED + fallbackAssignment=true) that:
     *  - was booked for the same (date, slot)
     *  - can fit in this room (capacity + facilities)
     *  - and the move is an improvement (new room capacity < current room capacity)
     *
     * @return the moved booking, or null if nothing moved
     */
    private Booking tryMoveEarliestFallbackBookingToRoom(Room targetRoom, LocalDate date, TimeSlot slot) {
        if (targetRoom.getStatus() == RoomStatus.UNDER_MAINTENANCE) {
            return null;
        }

        boolean slotTaken = bookingRepository
                .findByRoomAndDateAndSlotAndStatus(targetRoom, date, slot, BookingStatus.CONFIRMED)
                .isPresent();
        if (slotTaken) {
            return null;
        }

        List<Booking> fallbackBookings =
                bookingRepository.findByDateAndSlotAndStatusAndFallbackAssignmentTrueOrderByCreatedAtAsc(
                        date, slot, BookingStatus.CONFIRMED
                );

        for (Booking booking : fallbackBookings) {
            Room oldRoom = booking.getRoom();

            // Must be an improvement (smaller than the room they currently occupy)
            if (oldRoom != null && targetRoom.getCapacity() >= oldRoom.getCapacity()) {
                continue;
            }

            if (booking.getRequiredCapacity() > targetRoom.getCapacity()) {
                continue;
            }
            if (booking.isNeedsProjector() &&
                    !(targetRoom.isHasProjector() && targetRoom.isProjectorAvailable())) {
                continue;
            }
            if (booking.isNeedsAvSystem() &&
                    !(targetRoom.isHasAvSystem() && targetRoom.isAvSystemAvailable())) {
                continue;
            }

            // Cancel the old booking
            booking.setStatus(BookingStatus.CANCELLED);
            bookingRepository.save(booking);

            // Replicate booking creation logic
            Booking newBooking = bookingRepository
                .findByRoomAndDateAndSlot(targetRoom, date, slot)
                .orElseGet(Booking::new);

            newBooking.setRoom(targetRoom);
            newBooking.setDate(booking.getDate());
            newBooking.setSlot(booking.getSlot());
            newBooking.setBookedBy(booking.getBookedBy());
            newBooking.setRequiredCapacity(booking.getRequiredCapacity());
            newBooking.setNeedsProjector(booking.isNeedsProjector());
            newBooking.setNeedsAvSystem(booking.isNeedsAvSystem());
            newBooking.setStatus(BookingStatus.CONFIRMED);
            newBooking.setDurationHours(2);
            newBooking.setExtendedOnce(false);
            newBooking.setFallbackAssignment(targetRoom.getCapacity() > booking.getRequiredCapacity());
            newBooking.setCreatedAt(java.time.LocalDateTime.now());
            bookingRepository.save(newBooking);

            targetRoom.setStatus(RoomStatus.BOOKED);
            roomRepository.save(targetRoom);

            if (oldRoom != null) {
                oldRoom.setStatus(RoomStatus.AVAILABLE);
                roomRepository.save(oldRoom);
            }

            // Send notification to user of the new booking
            String subject = "Room Booking Reassigned";
            String bookingDetails = " Room ID: " + newBooking.getRoom().getId() + ", Date: " + newBooking.getDate() + ", Slot: " + newBooking.getSlot();
            String body = "Your booking has been reassigned to a new room. " + bookingDetails;
            boolean emailSent = Boolean.TRUE.equals(notificationClient.sendEmailByName(newBooking.getBookedBy(), subject, body).block());
            String emailStatus = emailSent ? " Email sent." : " Email sending failed.";

            // One room can host only one booking per (date, slot)
            return newBooking;
        }

        return null;
    }

}
