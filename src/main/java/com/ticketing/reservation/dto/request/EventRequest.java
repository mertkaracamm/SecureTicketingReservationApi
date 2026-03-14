package com.ticketing.reservation.dto.request;

import jakarta.validation.constraints.*;
import java.time.Instant;
import jakarta.validation.constraints.Future;

public record EventRequest(
        @NotBlank @Size(max = 500) String title,
        @NotBlank @Size(max = 500) String venue,
        @NotNull @Future Instant startsAt,
        @NotNull @Future Instant endsAt,
        @Min(1) int capacity
) {}