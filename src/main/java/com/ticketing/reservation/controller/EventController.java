package com.ticketing.reservation.controller;

import com.ticketing.reservation.dto.request.EventRequest;
import com.ticketing.reservation.dto.response.Responses.EventResponse;
import com.ticketing.reservation.dto.response.Responses.PageResponse;
import com.ticketing.reservation.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Parameter;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Event Management", description = "Event management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class EventController {

    private final EventService eventService;

    @PostMapping("/api/events")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @Operation(summary = "Create a draft event")
    public EventResponse createEvent(@Valid @RequestBody EventRequest request) {
        return eventService.createEvent(request);
    }

    @PutMapping("/api/events/{id}")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @Operation(summary = "Update an event")
    public EventResponse updateEvent(
            @Parameter(description = "Event ID", example = "9349a5fe-4240-4d43-ac80-f110ed77b8dd")
            @PathVariable UUID id,
            @Valid @RequestBody EventRequest request) {
        return eventService.updateEvent(id, request);
    }

    @PostMapping("/api/events/{id}/publish")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @Operation(summary = "Publish a draft event")
    public EventResponse publishEvent(
            @Parameter(description = "Event ID", example = "9349a5fe-4240-4d43-ac80-f110ed77b8dd")
            @PathVariable UUID id) {
        return eventService.publishEvent(id);
    }

    @GetMapping("/api/events")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @Operation(summary = "List events")
    public PageResponse<EventResponse> listEvents(
            @Parameter(description = "Filter by owner UUID", example = "4962d1c2-3d41-4ec8-9873-cfe834f79c62")
            @RequestParam(required = false) UUID ownerId,
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        return eventService.listEvents(ownerId, page, size);
    }

    @GetMapping("/api/events/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get event by ID")
    public EventResponse getEvent(
            @Parameter(description = "Event ID", example = "9349a5fe-4240-4d43-ac80-f110ed77b8dd")
            @PathVariable UUID id) {
        return eventService.getEvent(id);
    }

    @GetMapping("/api/events/public")
    @Operation(summary = "Browse published events - no auth required")
    public PageResponse<EventResponse> publicEvents(
            @Parameter(description = "Filter events starting after this date", example = "2026-01-01T00:00:00Z")
            @RequestParam(required = false) Instant from,
            @Parameter(description = "Filter events starting before this date", example = "2026-12-31T23:59:59Z")
            @RequestParam(required = false) Instant to,
            @Parameter(description = "Search by title or venue", example = "Istanbul")
            @RequestParam(required = false) String q,
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        return eventService.listPublicEvents(from, to, q, page, size);
    
    }
}