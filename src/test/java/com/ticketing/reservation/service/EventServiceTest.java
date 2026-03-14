package com.ticketing.reservation.service;

import com.ticketing.reservation.domain.entity.Event;
import com.ticketing.reservation.domain.entity.User;
import com.ticketing.reservation.dto.request.EventRequest;
import com.ticketing.reservation.exception.BusinessException;
import com.ticketing.reservation.exception.ResourceNotFoundException;
import com.ticketing.reservation.repository.EventRepository;
import com.ticketing.reservation.security.UserPrincipal;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock EventRepository eventRepository;
    @Mock AuditService auditService;
    @InjectMocks EventService eventService;

    private User organizerUser;
    private Event draftEvent;
    private EventRequest validRequest;

    @BeforeEach
    void setup() {
        organizerUser = User.builder()
                .id(UUID.randomUUID())
                .email("organizer@test.com")
                .passwordHash("hash")
                .roles("ORGANIZER")
                .build();

        draftEvent = Event.builder()
                .id(UUID.randomUUID())
                .ownerId(organizerUser.getId())
                .title("Test Event")
                .venue("Test Venue")
                .startsAt(Instant.now().plus(5, ChronoUnit.DAYS))
                .endsAt(Instant.now().plus(6, ChronoUnit.DAYS))
                .capacity(100)
                .published(false)
                .build();

        validRequest = new EventRequest(
                "Test Event",
                "Test Venue",
                Instant.now().plus(5, ChronoUnit.DAYS),
                Instant.now().plus(6, ChronoUnit.DAYS),
                100
        );

        setSecurityContext(organizerUser);
    }

    @AfterEach
    void teardown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createEvent_validRequest_shouldSucceed() {
        given(eventRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        var result = eventService.createEvent(validRequest);

        assertThat(result.title()).isEqualTo("Test Event");
        assertThat(result.venue()).isEqualTo("Test Venue");
        assertThat(result.capacity()).isEqualTo(100);
        assertThat(result.published()).isFalse();
        then(eventRepository).should().save(any(Event.class));
    }

    @Test
    void updateEvent_draftEvent_shouldSucceed() {
        given(eventRepository.findById(draftEvent.getId())).willReturn(Optional.of(draftEvent));
        given(eventRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        var updateRequest = new EventRequest(
                "Updated Title",
                "Updated Venue",
                Instant.now().plus(7, ChronoUnit.DAYS),
                Instant.now().plus(8, ChronoUnit.DAYS),
                200
        );

        var result = eventService.updateEvent(draftEvent.getId(), updateRequest);

        assertThat(result.title()).isEqualTo("Updated Title");
        assertThat(result.capacity()).isEqualTo(200);
    }

    @Test
    void updateEvent_publishedEvent_shouldThrowBusinessException() {
        draftEvent.setPublished(true);
        given(eventRepository.findById(draftEvent.getId())).willReturn(Optional.of(draftEvent));

        assertThatThrownBy(() -> eventService.updateEvent(draftEvent.getId(), validRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Published events cannot be modified");
    }

    @Test
    void publishEvent_draftEvent_shouldSucceed() {
        given(eventRepository.findById(draftEvent.getId())).willReturn(Optional.of(draftEvent));
        given(eventRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        var result = eventService.publishEvent(draftEvent.getId());

        assertThat(result.published()).isTrue();
    }

    @Test
    void publishEvent_alreadyPublished_shouldThrowBusinessException() {
        draftEvent.setPublished(true);
        given(eventRepository.findById(draftEvent.getId())).willReturn(Optional.of(draftEvent));

        assertThatThrownBy(() -> eventService.publishEvent(draftEvent.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already published");
    }

    @Test
    void updateEvent_notFound_shouldThrowResourceNotFoundException() {
        UUID randomId = UUID.randomUUID();
        given(eventRepository.findById(randomId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.updateEvent(randomId, validRequest))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createEvent_adminRole_shouldSucceed() {
        User adminUser = User.builder()
                .id(UUID.randomUUID())
                .email("admin@test.com")
                .passwordHash("hash")
                .roles("ADMIN")
                .build();
        setSecurityContext(adminUser);
        given(eventRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        var result = eventService.createEvent(validRequest);

        assertThat(result.title()).isEqualTo("Test Event");
    }

    private void setSecurityContext(User user) {
        UserPrincipal principal = new UserPrincipal(user);
        var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.setContext(new SecurityContextImpl(auth));
    }
}