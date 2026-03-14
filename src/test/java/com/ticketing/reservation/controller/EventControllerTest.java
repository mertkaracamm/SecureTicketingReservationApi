package com.ticketing.reservation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketing.reservation.domain.entity.User;
import com.ticketing.reservation.dto.request.EventRequest;
import com.ticketing.reservation.dto.request.LoginRequest;
import com.ticketing.reservation.repository.EventRepository;
import com.ticketing.reservation.repository.ReservationRepository;
import com.ticketing.reservation.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EventControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired EventRepository eventRepository;
    @Autowired ReservationRepository reservationRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    private String organizerToken;
    private String customerToken;
    private String adminToken;

    @BeforeEach
    void setup() throws Exception {
        reservationRepository.deleteAll();
        eventRepository.deleteAll();
        userRepository.deleteAll();

        userRepository.save(User.builder()
                .email("organizer@event.com")
                .passwordHash(passwordEncoder.encode("Test@12345!"))
                .roles("ORGANIZER")
                .build());

        userRepository.save(User.builder()
                .email("customer@event.com")
                .passwordHash(passwordEncoder.encode("Test@12345!"))
                .roles("CUSTOMER")
                .build());

        userRepository.save(User.builder()
                .email("admin@event.com")
                .passwordHash(passwordEncoder.encode("Test@12345!"))
                .roles("ADMIN")
                .build());

        organizerToken = login("organizer@event.com");
        customerToken = login("customer@event.com");
        adminToken = login("admin@event.com");
    }

    @Test
    @Order(1)
    void createEvent_asOrganizer_shouldReturn201() throws Exception {
        var request = new EventRequest(
                "Rock Concert",
                "Istanbul Arena",
                Instant.now().plus(10, ChronoUnit.DAYS),
                Instant.now().plus(10, ChronoUnit.DAYS).plus(3, ChronoUnit.HOURS),
                500
        );

        mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + organizerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Rock Concert"))
                .andExpect(jsonPath("$.published").value(false));
    }

    @Test
    @Order(2)
    void createEvent_asCustomer_shouldReturn403() throws Exception {
        var request = new EventRequest(
                "Unauthorized Event",
                "Some Venue",
                Instant.now().plus(10, ChronoUnit.DAYS),
                Instant.now().plus(11, ChronoUnit.DAYS),
                100
        );

        mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(3)
    void publishEvent_asOrganizer_shouldReturn200() throws Exception {
        //first creating event
        String eventId = createEventAndGetId(organizerToken);

        mockMvc.perform(post("/api/events/" + eventId + "/publish")
                        .header("Authorization", "Bearer " + organizerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.published").value(true));
    }

    @Test
    @Order(4)
    void publishEvent_alreadyPublished_shouldReturn422() throws Exception {
        String eventId = createEventAndGetId(organizerToken);

        // first publish
        mockMvc.perform(post("/api/events/" + eventId + "/publish")
                .header("Authorization", "Bearer " + organizerToken));

        // second publish → 422
        mockMvc.perform(post("/api/events/" + eventId + "/publish")
                        .header("Authorization", "Bearer " + organizerToken))
        				.andExpect(status().is(422));
    }

    @Test
    @Order(5)
    void listEvents_asOrganizer_shouldReturn200() throws Exception {
        createEventAndGetId(organizerToken);

        mockMvc.perform(get("/api/events")
                        .header("Authorization", "Bearer " + organizerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @Order(6)
    void getEvent_asAuthenticatedUser_shouldReturn200() throws Exception {
        String eventId = createEventAndGetId(organizerToken);

        mockMvc.perform(get("/api/events/" + eventId)
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(eventId));
    }

    @Test
    @Order(7)
    void listPublicEvents_noAuth_shouldReturn200() throws Exception {
        mockMvc.perform(get("/api/events/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @Order(8)
    void updateEvent_asDraftEvent_shouldReturn200() throws Exception {
        String eventId = createEventAndGetId(organizerToken);

        var updateRequest = new EventRequest(
                "Updated Concert",
                "New Venue",
                Instant.now().plus(15, ChronoUnit.DAYS),
                Instant.now().plus(16, ChronoUnit.DAYS),
                300
        );

        mockMvc.perform(put("/api/events/" + eventId)
                        .header("Authorization", "Bearer " + organizerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Concert"));
    }

    @Test
    @Order(9)
    void getEvent_notFound_shouldReturn404() throws Exception {
        mockMvc.perform(get("/api/events/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + organizerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(10)
    void createEvent_asAdmin_shouldReturn201() throws Exception {
        var request = new EventRequest(
                "Admin Event",
                "Admin Venue",
                Instant.now().plus(10, ChronoUnit.DAYS),
                Instant.now().plus(11, ChronoUnit.DAYS),
                100
        );

        mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    // --- helpers ---

    private String login(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, "Test@12345!"))))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("accessToken").asText();
    }

    private String createEventAndGetId(String token) throws Exception {
        var request = new EventRequest(
                "Test Event",
                "Test Venue",
                Instant.now().plus(10, ChronoUnit.DAYS),
                Instant.now().plus(11, ChronoUnit.DAYS),
                100
        );
        MvcResult result = mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asText();
    }
}