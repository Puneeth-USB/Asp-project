package com.ASP.room.service.DTO;

import com.ASP.room.service.Entity.FacilityScope;
import jakarta.validation.constraints.NotNull;

public record MaintenanceBlockRequestDTO(@NotNull FacilityScope scope) {
}

