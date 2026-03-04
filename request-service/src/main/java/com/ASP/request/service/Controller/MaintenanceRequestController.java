package com.ASP.request.service.Controller;

import com.ASP.request.service.DTO.MaintenanceRequestDTO;
import com.ASP.request.service.Entity.MaintenanceRequest;
import com.ASP.request.service.Entity.MaintenanceStaff;
import com.ASP.request.service.Service.MaintenanceRequestService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/requests")
public class MaintenanceRequestController {

    private final MaintenanceRequestService service;

    public MaintenanceRequestController(MaintenanceRequestService service) {
        this.service = service;
    }


    @PostMapping
    public ResponseEntity<MaintenanceRequest> create(
            @RequestBody @Valid MaintenanceRequestDTO dto) {
        MaintenanceRequest saved = service.createRequest(dto);
        return ResponseEntity.ok(saved);
    }


    @PutMapping("/{id}/assign/{staffId}")
    public ResponseEntity<MaintenanceRequest> assignStaff(
            @PathVariable Long id,
            @PathVariable Long staffId) {
        MaintenanceRequest updated = service.assignStaff(id, staffId);
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/{id}/complete")
    public ResponseEntity<MaintenanceRequest> complete(@PathVariable Long id) {
        MaintenanceRequest updated = service.completeRequest(id);
        return ResponseEntity.ok(updated);
    }

    @GetMapping
    public List<MaintenanceRequest> list() {
        return service.getAll();
    }


    @GetMapping("/by-name/{name}")
    public List<MaintenanceRequest> getByName(@PathVariable("name") String name) {
        return service.getByBookedBy(name);
    }


    @PutMapping("/{id}")
    public ResponseEntity<MaintenanceRequest> update(
            @PathVariable Long id,
            @RequestBody @Valid MaintenanceRequestDTO dto) {
        MaintenanceRequest updated = service.updateRequest(id, dto);
        return ResponseEntity.ok(updated);
    }


    @GetMapping("/staff")
    public List<MaintenanceStaff> getAllStaff() {
        return service.getAllStaff();
    }

    @PostMapping("/add/staff")
    public ResponseEntity<MaintenanceStaff> createStaff(@RequestBody @Valid MaintenanceStaff staff) {
        MaintenanceStaff created = service.createStaff(staff);
        return ResponseEntity.ok(created);
    }

    @GetMapping("/unassigned")
    public List<MaintenanceRequest> getUnassignedRequests() {
        return service.getUnassignedRequests();
    }

    @GetMapping("/staff/{staffId}/assigned")
    public List<MaintenanceRequest> getAssignedRequestsByStaff(@PathVariable Long staffId) {
        return service.getAssignedRequestsByStaffId(staffId); // currently returns IN_PROGRESS
    }

    @GetMapping("/staff/{staffName}/in-progress")
    public List<MaintenanceRequest> getInProgressRequests(@PathVariable String staffName) {
        return service.getInProgressByStaffName(staffName);
    }

    @GetMapping("/staff/{staffName}/completed")
    public List<MaintenanceRequest> getCompletedRequestsByStaff(@PathVariable String staffName) {
        return service.getCompletedByStaffName(staffName);
    }
    @GetMapping("/staff/{expertise}")
    public List<MaintenanceStaff>  getStaffPerExpertise(@PathVariable String expertise) {
        return service.getStaffPerExpertise(expertise);

    }

}