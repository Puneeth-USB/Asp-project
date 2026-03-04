package com.ASP.request.service.Repository;

import com.ASP.request.service.Entity.MaintenanceStaff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaintenanceStaffRepo  extends JpaRepository<MaintenanceStaff, Long> {
    List<MaintenanceStaff> findByNameIgnoreCase(String name);
}
