package com.ticketing.reservation.aspect;

import com.ticketing.reservation.dto.response.Responses.EventResponse;
import com.ticketing.reservation.dto.response.Responses.ReservationResponse;
import com.ticketing.reservation.dto.response.Responses.UserResponse;
import com.ticketing.reservation.security.UserPrincipal;
import com.ticketing.reservation.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

// AOP-based audit logging - intercepts service methods automatically
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditService auditService;

    @AfterReturning(
        pointcut = "execution(* com.ticketing.reservation.service.AuthService.login(..)) || " +
                   "execution(* com.ticketing.reservation.service.AuthService.register(..)) || " +
                   "execution(* com.ticketing.reservation.service.EventService.createEvent(..)) || " +
                   "execution(* com.ticketing.reservation.service.EventService.updateEvent(..)) || " +
                   "execution(* com.ticketing.reservation.service.EventService.publishEvent(..)) || " +
                   "execution(* com.ticketing.reservation.service.ReservationService.createReservation(..)) || " +
                   "execution(* com.ticketing.reservation.service.ReservationService.confirmReservation(..)) || " +
                   "execution(* com.ticketing.reservation.service.ReservationService.cancelReservation(..))",
        returning = "result"
    )
    public void auditAction(JoinPoint jp, Object result) {
        try {
            UUID actorId = extractActorId();
            String action = jp.getSignature().getName().toUpperCase();
            String resourceType = deriveResource(jp.getSignature().getDeclaringTypeName());
            String resourceId = extractResourceId(result);
            HttpServletRequest req = currentRequest();
            String ip = req != null ? req.getRemoteAddr() : "unknown";
            String ua = req != null ? req.getHeader("User-Agent") : "unknown";
            auditService.log(actorId, action, resourceType, resourceId, ip, ua);
        } catch (Exception ignored) {
            // Never let audit failures break the main flow
        }
    }

    private UUID extractActorId() {
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof UserPrincipal p) {
                return p.getId();
            }
        } catch (Exception ignored) {} // Audit failure must never affect the main operation
        return null;
    }

    private String deriveResource(String className) {
        if (className.contains("Auth")) return "USER";
        if (className.contains("Event")) return "EVENT";
        if (className.contains("Reservation")) return "RESERVATION";
        return "UNKNOWN";
    }

    private String extractResourceId(Object result) {
        if (result instanceof EventResponse r) return r.id().toString();
        if (result instanceof ReservationResponse r) return r.id().toString();
        if (result instanceof UserResponse r) return r.id().toString();
        return null;
    }

    private HttpServletRequest currentRequest() {
        try {
            return ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        } catch (Exception e) {
            return null;
        }
    }
}