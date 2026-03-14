package com.ticketing.reservation.service;

import com.ticketing.reservation.domain.entity.AuditLog;
import com.ticketing.reservation.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


import java.util.UUID;

// Async audit logging - never blocks main transaction
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(UUID actorId, String action, String resourceType,
                    String resourceId, String ip, String userAgent) {
        AuditLog log = AuditLog.builder()
                .actorId(actorId)
                .action(action)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .ip(ip)
                .userAgent(userAgent)
                .build();
        auditLogRepository.save(log);
    }
}