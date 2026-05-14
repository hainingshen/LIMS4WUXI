package com.njxs.lims.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

  public static final String TRACE_ID_ATTRIBUTE = "traceId";
  public static final String TRACE_ID_HEADER = "X-Trace-Id";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain
  ) throws ServletException, IOException {
    String traceId = resolveTraceId(request);
    request.setAttribute(TRACE_ID_ATTRIBUTE, traceId);
    response.setHeader(TRACE_ID_HEADER, traceId);
    MDC.put(TRACE_ID_ATTRIBUTE, traceId);
    try {
      filterChain.doFilter(request, response);
    } finally {
      MDC.remove(TRACE_ID_ATTRIBUTE);
    }
  }

  private String resolveTraceId(HttpServletRequest request) {
    String header = request.getHeader(TRACE_ID_HEADER);
    if (header == null || header.isBlank()) {
      return UUID.randomUUID().toString();
    }
    return header;
  }
}
