package com.ASP.notification.DTO;

import jakarta.validation.constraints.NotNull;

public record EmailNotification(@NotNull String to,  String subject, @NotNull String body) {
}
