package com.ticketing.reservation.config;

import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.*;
import io.swagger.v3.oas.models.security.*;
import io.swagger.v3.oas.models.tags.Tag;

import java.util.List;

import org.springframework.context.annotation.*;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Secure Ticketing & Reservation API")
                        .description("""
                                Event ticketing platform with JWT authentication,
                                role-based authorization, idempotent reservations
                                and oversell prevention.
                                
                                **Roles:** ADMIN | ORGANIZER | CUSTOMER
                                
                                **Auth:** Register → Login → Bearer token
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Mert Karaçam")
                                .email("mert.karacamm@gmail.com")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT access token")))
                .tags(List.of(
                	    new Tag().name("Authentication & Authorization").description("Register, login and token refresh"),
                	    new Tag().name("Event Management").description("Event management endpoints"),
                	    new Tag().name("Reservations & Discovery").description("Reservation lifecycle endpoints")
                	));
    }
}