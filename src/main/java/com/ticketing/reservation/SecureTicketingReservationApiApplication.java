package com.ticketing.reservation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@EnableConfigurationProperties
@EnableCaching
public class SecureTicketingReservationApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(SecureTicketingReservationApiApplication.class, args);
    }
}