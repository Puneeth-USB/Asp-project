package com.ASP.notification.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

public record ValidateDTO(@Email @NotBlank String email,
                          @NotBlank String otp) {
}
