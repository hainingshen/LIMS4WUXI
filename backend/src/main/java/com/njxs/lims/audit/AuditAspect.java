package com.njxs.lims.audit;

import com.njxs.lims.auth.domain.AuthPrincipal;
import com.njxs.lims.auth.domain.ClientType;
import com.njxs.lims.auth.service.AuthContext;
import com.njxs.lims.common.exception.BusinessException;
import com.njxs.lims.common.web.TraceIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 100)
@ConditionalOnProperty(prefix = "lims.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AuditAspect {

  private static final int MAX_FAILURE_REASON_LENGTH = 1000;

  private final AuditLogRepository auditLogRepository;

  public AuditAspect(AuditLogRepository auditLogRepository) {
    this.auditLogRepository = auditLogRepository;
  }

  @Around("@annotation(audited)")
  public Object record(ProceedingJoinPoint pjp, Audited audited) throws Throwable {
    try {
      Object result = pjp.proceed();
      append(pjp.getArgs(), audited, "SUCCESS", null);
      return result;
    } catch (Throwable throwable) {
      append(pjp.getArgs(), audited, "FAILURE", failureReason(throwable));
      throw throwable;
    }
  }

  private void append(Object[] args, Audited audited, String result, String failureReason) {
    Optional<AuthPrincipal> principal = AuthContext.current();
    HttpServletRequest request = currentRequest().orElse(null);
    auditLogRepository.append(new AuditLogEntry(
        UUID.randomUUID(),
        Instant.now(),
        principal.map(AuthPrincipal::userId).orElse(null),
        principal.flatMap(this::firstRole).orElse(null),
        principal.map(AuthPrincipal::clientType).orElse(ClientType.SYSTEM),
        actionCode(args, audited),
        audited.entityType(),
        firstUuid(args).orElse(null),
        null,
        result,
        failureReason,
        clientIp(request),
        userAgent(request),
        traceId(request)
    ));
  }

  private Optional<UUID> firstUuid(Object[] args) {
    return Arrays.stream(args)
        .filter(UUID.class::isInstance)
        .map(UUID.class::cast)
        .findFirst();
  }

  private String actionCode(Object[] args, Audited audited) {
    return Arrays.stream(args)
        .filter(AuditActionProvider.class::isInstance)
        .map(AuditActionProvider.class::cast)
        .map(AuditActionProvider::auditActionCode)
        .filter(actionCode -> actionCode != null && !actionCode.isBlank())
        .findFirst()
        .orElse(audited.actionCode());
  }

  private Optional<String> firstRole(AuthPrincipal principal) {
    return principal.roles().stream().findFirst();
  }

  private Optional<HttpServletRequest> currentRequest() {
    if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
      return Optional.of(attributes.getRequest());
    }
    return Optional.empty();
  }

  private String clientIp(HttpServletRequest request) {
    if (request == null) {
      return null;
    }
    String forwardedFor = request.getHeader("X-Forwarded-For");
    if (forwardedFor != null && !forwardedFor.isBlank()) {
      return forwardedFor.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }

  private String userAgent(HttpServletRequest request) {
    return request == null ? null : request.getHeader("User-Agent");
  }

  private String traceId(HttpServletRequest request) {
    if (request == null) {
      return null;
    }
    Object value = request.getAttribute(TraceIdFilter.TRACE_ID_ATTRIBUTE);
    if (value instanceof String traceId && !traceId.isBlank()) {
      return traceId;
    }
    return request.getHeader(TraceIdFilter.TRACE_ID_HEADER);
  }

  private String failureReason(Throwable throwable) {
    if (throwable instanceof BusinessException businessException) {
      return truncate(businessException.getCode() + ": " + businessException.getMessage());
    }
    String reason = throwable.getClass().getSimpleName() + ": " + throwable.getMessage();
    return truncate(reason);
  }

  private String truncate(String reason) {
    if (reason.length() <= MAX_FAILURE_REASON_LENGTH) {
      return reason;
    }
    return reason.substring(0, MAX_FAILURE_REASON_LENGTH);
  }
}
