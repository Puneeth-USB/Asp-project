package com.ASP.room.service.Repository;

import com.ASP.room.service.Entity.PendingRequest;
import com.ASP.room.service.Entity.TimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PendingRequestsRepo extends JpaRepository<PendingRequest, Long> {
    List<PendingRequest> findByDateAndSlotOrderByCreatedAt(LocalDate date, TimeSlot slot);
    List<PendingRequest> findByRequestedByOrderByCreatedAtAsc(String requestedBy);
}
