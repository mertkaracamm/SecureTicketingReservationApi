package com.ticketing.reservation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketing.reservation.dto.request.LoginRequest;
import com.ticketing.reservation.dto.request.RegisterRequest;
import com.ticketing.reservation.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthControllerTest {

    @Autowired MockMvc mockMvc;    
    @Autowired UserRepository userRepository;
    
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @BeforeEach
    void cleanup() {
        userRepository.deleteAll();
    }

    @Test
    @Order(1)
    void register_shouldReturn201() throws Exception {
        var request = new RegisterRequest("test@example.com", "Test@12345!", null);
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.roles").value("CUSTOMER"));
    }

    @Test
    @Order(2)
    void register_duplicateEmail_shouldReturn409() throws Exception {
        var request = new RegisterRequest("dup@example.com", "Test@12345!", null);
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @Order(3)
    void login_validCredentials_shouldReturnTokens() throws Exception {
        var reg = new RegisterRequest("login@example.com", "Test@12345!", null);
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reg)));

        var login = new LoginRequest("login@example.com", "Test@12345!");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists());
    }

    @Test
    @Order(4)
    void login_wrongPassword_shouldReturn401() throws Exception {
        var reg = new RegisterRequest("wrong@example.com", "Test@12345!", null);
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reg)));

        var login = new LoginRequest("wrong@example.com", "WrongPassword!");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status != 401 && status != 429) {
                        throw new AssertionError("Expected 401 or 429 but was: " + status);
                    }
                });
    }

    @Test
    void register_weakPassword_shouldReturn400() throws Exception {
        var request = new RegisterRequest("weak@example.com", "password", null);
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}