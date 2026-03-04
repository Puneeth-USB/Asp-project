package com.ASP.request.service.Repository;

import com.ASP.request.service.Entity.MaintenanceRequest;
import com.ASP.request.service.Entity.MaintenanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaintenanceRequestRepo extends JpaRepository<MaintenanceRequest, Long> {
    List<MaintenanceRequest> findByBookedByIgnoreCaseOrderByCreatedAtDesc(String bookedBy);

    List<MaintenanceRequest> findByAssignedStaffIdIsNullOrderByCreatedAtDesc();

    List<MaintenanceRequest> findByAssignedStaffIdAndStatusOrderByCreatedAtDesc(Long staffId, MaintenanceStatus status);

    List<MaintenanceRequest> findByAssignedStaffIdAndStatusOrderByCompletedAtDesc(Long staffId, MaintenanceStatus status);

    List<MaintenanceRequest> findByAssignedStaffIdInAndStatusOrderByCreatedAtDesc(List<Long> staffIds, MaintenanceStatus status);
    List<MaintenanceRequest> findByAssignedStaffIdInAndStatusOrderByCompletedAtDesc(List<Long> staffIds, MaintenanceStatus status);
}
