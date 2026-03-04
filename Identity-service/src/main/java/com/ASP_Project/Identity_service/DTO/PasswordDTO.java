package com.ASP_Project.Identity_service.DTO;

import jakarta.validation.constraints.NotBlank;

public record PasswordDTO(@NotBlank String email, @NotBlank String password) {}

