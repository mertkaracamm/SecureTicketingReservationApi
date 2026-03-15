package com.ticketing.reservation.service;

import com.ticketing.reservation.domain.entity.Event;
import com.ticketing.reservation.dto.request.EventRequest;
import com.ticketing.reservation.dto.response.Responses;
import com.ticketing.reservation.dto.response.Responses.EventResponse;
import com.ticketing.reservation.dto.response.Responses.PageResponse;
import com.ticketing.reservation.exception.*;
import com.ticketing.reservation.repository.EventRepository;
import com.ticketing.reservation.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;

    @CacheEvict(value = "events", allEntries = true)
    @Transactional
    public EventResponse createEvent(EventRequest request) {
        UserPrincipal principal = currentUser();
        validateDates(request);

        Event event = Event.builder()
                .ownerId(principal.getId())
                .title(request.title())
                .venue(request.venue())
                .startsAt(request.startsAt())
                .endsAt(request.endsAt())
                .capacity(request.capacity())
                .published(false)
                .build();
        return EventResponse.from(eventRepository.save(event));
    }

    @CacheEvict(value = "events", allEntries = true)
    @Transactional
    public EventResponse updateEvent(UUID id, EventRequest request) {
        Event event = findAndVerifyOwnership(id);
        if (event.isPublished()) {
            throw new BusinessException("Published events cannot be modified");
        }
        validateDates(request);
        event.setTitle(request.title());
        event.setVenue(request.venue());
        event.setStartsAt(request.startsAt());
        event.setEndsAt(request.endsAt());
        event.setCapacity(request.capacity());
        return EventResponse.from(eventRepository.save(event));
    }

    @CacheEvict(value = "events", allEntries = true)
    @Transactional
    public EventResponse publishEvent(UUID id) {
        Event event = findAndVerifyOwnership(id);
        if (event.isPublished()) {
            throw new BusinessException("Event is already published");
        }
        if (event.getStartsAt().isBefore(Instant.now())) {
            throw new BusinessException("Cannot publish an event that has already started");
        }
        event.setPublished(true);
        return EventResponse.from(eventRepository.save(event));
    }

    // Admins see all events; organizers are restricted to their own
    @Transactional(readOnly = true)
    public PageResponse<EventResponse> listEvents(UUID ownerId, int page, int size) {
        UserPrincipal principal = currentUser();
        boolean isAdmin = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Event> events;

        if (isAdmin && ownerId == null) {
            events = eventRepository.findAll(pageable);
        } else {
            UUID filterOwner = isAdmin ? ownerId : principal.getId();
            events = eventRepository.findByOwnerId(filterOwner, pageable);
        }
        return toPage(events.map(EventResponse::from), pageable);
    }

    // Result is cached in Redis for 5 min — invalidated on any create/update/publish
    @Cacheable(value = "events", key = "#from + '-' + #to + '-' + #q + '-' + #page + '-' + #size")
    @Transactional(readOnly = true)
    public Responses.PageResponse<Responses.EventResponse> listPublicEvents(Instant from, Instant to, String q, int page, int size) {
        long offset = (long) page * size;
        List<Event> events = eventRepository.findPublished(from, to, q, size, offset);
        List<Responses.EventResponse> content = events.stream().map(Responses.EventResponse::from).toList();
        return new Responses.PageResponse<>(content, page, size, content.size(), 1);
    }

    @Transactional(readOnly = true)
    public EventResponse getEvent(UUID id) {
        return EventResponse.from(eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + id)));
    }

    private Event findAndVerifyOwnership(UUID id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + id));
        UserPrincipal principal = currentUser();
        boolean isAdmin = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin && !event.getOwnerId().equals(principal.getId())) {
            throw new AccessDeniedException("You do not own this event");
        }
        return event;
    }

    private void validateDates(EventRequest request) {
        if (!request.endsAt().isAfter(request.startsAt())) {
            throw new BusinessException("Event end time must be after start time");
        }
    }

    private UserPrincipal currentUser() {
        return (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private <T> PageResponse<T> toPage(Page<T> page, Pageable pageable) {
        return new PageResponse<>(page.getContent(), pageable.getPageNumber(),
                pageable.getPageSize(), page.getTotalElements(), page.getTotalPages());
    }
}