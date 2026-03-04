package com.ASP.request.service.DTO;

import com.ASP.request.service.Entity.FacilityScope;
import com.ASP.request.service.Entity.PriorityLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MaintenanceRequestDTO(@NotNull Long roomId,
                                    @NotBlank String bookedBy,
                                    @NotNull FacilityScope scope,
                                    @NotNull PriorityLevel priority,
                                    @NotBlank String description) {
}
