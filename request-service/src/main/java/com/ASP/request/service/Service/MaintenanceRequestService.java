package com.ASP.request.service.Service;

import com.ASP.request.service.DTO.MaintenanceBlockRequestDTO;
import com.ASP.request.service.DTO.MaintenanceRequestDTO;
import com.ASP.request.service.Entity.*;
import com.ASP.request.service.Repository.MaintenanceRequestRepo;
import com.ASP.request.service.Repository.MaintenanceStaffRepo;
import com.ASP.request.service.client.RoomServiceClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MaintenanceRequestService {

    private final MaintenanceRequestRepo requestRepository;
    private final MaintenanceStaffRepo staffRepository;
    private final RoomServiceClient roomServiceClient;

    public MaintenanceRequestService(MaintenanceRequestRepo requestRepository,
                                     MaintenanceStaffRepo staffRepository,
                                     RoomServiceClient roomServiceClient) {
        this.requestRepository = requestRepository;
        this.staffRepository = staffRepository;
        this.roomServiceClient = roomServiceClient;
    }

    @Transactional
    public MaintenanceRequest createRequest(MaintenanceRequestDTO dto) {

        MaintenanceRequest request = new MaintenanceRequest();
        request.setRoomId(dto.roomId());
        request.setBookedBy(dto.bookedBy());
        request.setScope(dto.scope());
        request.setPriority(dto.priority());
        request.setDescription(dto.description());
        request.setStatus(MaintenanceStatus.OPEN);
        request.setCreatedAt(LocalDateTime.now());
        request.setAssignedStaffId(null);

        MaintenanceRequest saved = requestRepository.save(request);

        roomServiceClient.blockRoomOrFacility(
                dto.roomId(),
                new MaintenanceBlockRequestDTO(dto.scope())
        ).doOnError(e -> System.err.println("Error blocking room/facility: " + e.getMessage()))
         .subscribe();

        return saved;
    }

    @Transactional
    public MaintenanceRequest assignStaff(Long requestId, Long staffId) {
        MaintenanceRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Maintenance request not found"));

        MaintenanceStaff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new IllegalArgumentException("Maintenance staff not found"));

        // Example: max 5 requests per staff (customize as needed)
        int maxRequests = 5;
        if (staff.getAssignedRequestCount() >= maxRequests) {
            throw new IllegalStateException("Staff member is overloaded with requests");
        }

        // If already assigned to someone else, decrement old staff's count
        if (request.getAssignedStaffId() != null && !request.getAssignedStaffId().equals(staffId)) {
            MaintenanceStaff oldStaff = staffRepository.findById(request.getAssignedStaffId())
                    .orElse(null);
            if (oldStaff != null && oldStaff.getAssignedRequestCount() > 0) {
                oldStaff.setAssignedRequestCount(oldStaff.getAssignedRequestCount() - 1);
                staffRepository.save(oldStaff);
            }
        }

        request.setAssignedStaffId(staff.getId());
        request.setStatus(MaintenanceStatus.IN_PROGRESS);

        // increment this staff's count
        staff.setAssignedRequestCount(staff.getAssignedRequestCount() + 1);
        staffRepository.save(staff);

        try {
            return requestRepository.save(request);
        } catch (Exception e) {
            // Rollback staff count increment if request save fails
            staff.setAssignedRequestCount(staff.getAssignedRequestCount() - 1);
            staffRepository.save(staff);
            throw new RuntimeException("Failed to assign staff to request", e);
        }
    }

    @Transactional
    public MaintenanceRequest completeRequest(Long id) {
        MaintenanceRequest request = requestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Request not found"));

        request.setStatus(MaintenanceStatus.COMPLETED);
        request.setCompletedAt(LocalDateTime.now());

        // decrement count for assigned staff (if any)
        if (request.getAssignedStaffId() != null) {
            staffRepository.findById(request.getAssignedStaffId()).ifPresent(staff -> {
                int current = staff.getAssignedRequestCount();
                if (current > 0) {
                    staff.setAssignedRequestCount(current - 1);
                    staffRepository.save(staff);
                }
            });
        }

        MaintenanceRequest saved = requestRepository.save(request);

        // notify room-service that maintenance is done
        roomServiceClient.unblockRoomOrFacility(
                request.getRoomId(),
                new MaintenanceBlockRequestDTO(request.getScope())
        ).doOnError(e -> System.err.println("Error unblocking room/facility: " + e.getMessage()))
         .subscribe();

        return saved;
    }

    public List<MaintenanceRequest> getAll() {
        return requestRepository.findAll();
    }

    /** 🔹 New: get all requests for a given bookedBy name */
    public List<MaintenanceRequest> getByBookedBy(String bookedBy) {
        return requestRepository.findByBookedByIgnoreCaseOrderByCreatedAtDesc(bookedBy);
    }

    /** 🔹 New: update a request by id (scope/priority/description/bookedBy) */
    @Transactional
    public MaintenanceRequest updateRequest(Long id, MaintenanceRequestDTO dto) {
        MaintenanceRequest request = requestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Request not found"));

        FacilityScope oldScope = request.getScope();

        // we do NOT change roomId here to avoid complex re-blocking logic
        request.setBookedBy(dto.bookedBy());
        request.setScope(dto.scope());
        request.setPriority(dto.priority());
        request.setDescription(dto.description());

        MaintenanceRequest saved = requestRepository.save(request);

        // If scope changed, adjust blocking in room-service
        if (oldScope != dto.scope()) {
            // unblock old scope
            roomServiceClient.unblockRoomOrFacility(
                    request.getRoomId(),
                    new MaintenanceBlockRequestDTO(oldScope)
            );
            // block new scope
            roomServiceClient.blockRoomOrFacility(
                    request.getRoomId(),
                    new MaintenanceBlockRequestDTO(dto.scope())
            );
        }

        return saved;
    }

    public List<MaintenanceStaff> getAllStaff() {
        return staffRepository.findAll();
    }

    public List<MaintenanceRequest> getUnassignedRequests() {
        return requestRepository.findByAssignedStaffIdIsNullOrderByCreatedAtDesc();
    }

    public MaintenanceStaff createStaff(MaintenanceStaff staff) {
        staff.setId(null); // Ensure ID is auto-generated
        staff.setAssignedRequestCount(0); // Initialize count to 0
        return staffRepository.save(staff);
    }

    public List<MaintenanceRequest> getAssignedRequestsByStaffId(Long staffId) {
        return requestRepository.findByAssignedStaffIdAndStatusOrderByCreatedAtDesc(
                staffId, MaintenanceStatus.IN_PROGRESS);
    }

    public List<MaintenanceRequest> getCompletedRequestsByStaffId(Long staffId) {
        return requestRepository.findByAssignedStaffIdAndStatusOrderByCompletedAtDesc(
                staffId, MaintenanceStatus.COMPLETED);
    }

    public List<MaintenanceRequest> getInProgressByStaffName(String staffName) {
        List<MaintenanceStaff> staff = staffRepository.findByNameIgnoreCase(staffName);
        if (staff == null || staff.isEmpty()) return List.of();
        List<Long> ids = staff.stream().map(MaintenanceStaff::getId).toList();
        return requestRepository.findByAssignedStaffIdInAndStatusOrderByCreatedAtDesc(ids, MaintenanceStatus.IN_PROGRESS);
    }

    public List<MaintenanceRequest> getCompletedByStaffName(String staffName) {
        List<MaintenanceStaff> staff = staffRepository.findByNameIgnoreCase(staffName);
        if (staff == null || staff.isEmpty()) return List.of();
        List<Long> ids = staff.stream().map(MaintenanceStaff::getId).toList();
        return requestRepository.findByAssignedStaffIdInAndStatusOrderByCompletedAtDesc(ids, MaintenanceStatus.COMPLETED);
    }

    public List<MaintenanceStaff> getStaffPerExpertise(String expertise) {
        ExpertiseType type = ExpertiseType.valueOf(expertise.trim().toUpperCase());
        return staffRepository.findAll().stream()
                .filter(s -> s.getExpertises().contains(ExpertiseType.GENERAL_ROOM)
                        || s.getExpertises().contains(type))
                .toList();


    }
}

