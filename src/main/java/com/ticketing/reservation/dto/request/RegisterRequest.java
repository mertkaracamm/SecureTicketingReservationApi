package com.ticketing.reservation.dto.request;

import jakarta.validation.constraints.*;

public record RegisterRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 100)
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).+$",
                 message = "Password must contain uppercase, lowercase, digit and special character")
        String password,
        String role
) {}