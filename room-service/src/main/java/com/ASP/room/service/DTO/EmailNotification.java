package com.ASP.room.service.DTO;

import jakarta.validation.constraints.NotNull;

public record EmailNotification(@NotNull String to, String subject, @NotNull String body) {
}
