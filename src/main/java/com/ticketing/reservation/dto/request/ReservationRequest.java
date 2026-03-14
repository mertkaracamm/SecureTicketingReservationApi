package com.ticketing.reservation.dto.request;

import jakarta.validation.constraints.Min;

public record ReservationRequest(@Min(1) int seats) {}