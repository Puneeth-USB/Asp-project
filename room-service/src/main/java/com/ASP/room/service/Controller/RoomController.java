package com.ASP.room.service.Controller;

import com.ASP.room.service.DTO.BookingResponseDTO;
import com.ASP.room.service.DTO.MaintenanceBlockRequestDTO;
import com.ASP.room.service.DTO.RoomBookingRequestDTO;
import com.ASP.room.service.Entity.PendingRequest;
import com.ASP.room.service.Entity.Room;
import com.ASP.room.service.Entity.RoomStatus;
import com.ASP.room.service.Repository.RoomRepo;
import com.ASP.room.service.Service.RoomAllocationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomRepo roomRepository;
    private final RoomAllocationService allocationService;

    public RoomController(RoomRepo roomRepository,
                          RoomAllocationService allocationService) {
        this.roomRepository = roomRepository;
        this.allocationService = allocationService;
    }

    // -------- Room CRUD -------------

    @PostMapping("/add")
    public ResponseEntity<Room> createRoom(@RequestBody @Valid Room room) {
        room.setId(null); // id (room number) auto generated
        Room saved = roomRepository.save(room);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/all")
    public List<Room> getRooms() {
        return roomRepository.findAll();
    }

    @GetMapping("/available")
    public List<Room> getAvailableRooms() {
        return roomRepository.findByStatusIn(List.of(RoomStatus.AVAILABLE, RoomStatus.BOOKED));
    }

    // -------- Booking ---------------

    @PostMapping("/bookings")
    public ResponseEntity<BookingResponseDTO> bookRoom(
            @RequestBody @Valid RoomBookingRequestDTO request) {
        BookingResponseDTO response = allocationService.bookRoom(request);
        return ResponseEntity.ok(response);
    }

    // Extend booking by 1 hour (only once)
    @PutMapping("/bookings/{bookingId}/extend")
    public ResponseEntity<BookingResponseDTO> extendBooking(
            @PathVariable Long bookingId) {
        BookingResponseDTO response = allocationService.extendBookingOneHour(bookingId);
        return ResponseEntity.ok(response);
    }

    // -------- Maintenance integration (called by maintenance-service) -----

    @PostMapping("/{roomId}/maintenance/block")
    public ResponseEntity<Void> blockRoomOrFacility(
            @PathVariable Long roomId,
            @RequestBody @Valid MaintenanceBlockRequestDTO request) {
        allocationService.blockRoomOrFacility(roomId, request.scope());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{roomId}/maintenance/unblock")
    public ResponseEntity<Void> unblockRoomOrFacility(
            @PathVariable Long roomId,
            @RequestBody @Valid MaintenanceBlockRequestDTO request) {
        allocationService.unblockRoomOrFacility(roomId, request.scope());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/booked-by")
    public ResponseEntity<List<Room>> getRoomsBookedByName(
            @RequestParam("name") String name) {

        List<Room> rooms = allocationService.getCurrentlyBookedRoomsFor(name);
        return ResponseEntity.ok(rooms);
    }

    @PostMapping("/bookings/{bookingId}/release")
    public ResponseEntity<BookingResponseDTO> releaseRoom(
            @PathVariable Long bookingId) {

        BookingResponseDTO response = allocationService.releaseRoomAndAssignIfPossible(bookingId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/pending-requests")
    public ResponseEntity<List<PendingRequest>> getPendingRequestsByName(
            @RequestParam("name") String name) {

        List<PendingRequest> pendingRequests = allocationService.getPendingRequestsForName(name);
        return ResponseEntity.ok(pendingRequests);
    }
}
