package com.njxs.lims.common.web;

import com.njxs.lims.auth.service.AuthenticationFailedException;
import com.njxs.lims.common.api.ApiError;
import com.njxs.lims.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ApiError> handleBusinessException(
      BusinessException exception,
      HttpServletRequest request
  ) {
    return ResponseEntity
        .status(exception.getStatus())
        .body(error(exception.getCode(), exception.getMessage(), request, exception.getDetails()));
  }

  @ExceptionHandler(AuthenticationFailedException.class)
  public ResponseEntity<ApiError> handleAuthenticationFailed(
      AuthenticationFailedException exception,
      HttpServletRequest request
  ) {
    return ResponseEntity
        .status(HttpStatus.UNAUTHORIZED)
        .body(error(exception.getCode(), exception.getMessage(), request, Map.of()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> handleMethodArgumentNotValid(
      MethodArgumentNotValidException exception,
      HttpServletRequest request
  ) {
    Map<String, Object> details = new LinkedHashMap<>();
    for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
      details.put(fieldError.getField(), fieldError.getDefaultMessage());
    }
    return ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .body(error("VALIDATION_FAILED", "请求参数校验失败", request, details));
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ApiError> handleConstraintViolation(
      ConstraintViolationException exception,
      HttpServletRequest request
  ) {
    return ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .body(error("VALIDATION_FAILED", "请求参数校验失败", request, Map.of("violations", exception.getMessage())));
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<ApiError> handleNoResourceFound(
      NoResourceFoundException exception,
      HttpServletRequest request
  ) {
    return ResponseEntity
        .status(HttpStatus.NOT_FOUND)
        .body(error("NOT_FOUND", "资源不存在", request, Map.of("path", request.getRequestURI())));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiError> handleException(Exception exception, HttpServletRequest request) {
    return ResponseEntity
        .status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(error("INTERNAL_SERVER_ERROR", "服务器内部错误", request, Map.of()));
  }

  private ApiError error(
      String code,
      String message,
      HttpServletRequest request,
      Map<String, Object> details
  ) {
    return new ApiError(code, message, traceId(request), details == null ? Map.of() : details);
  }

  private String traceId(HttpServletRequest request) {
    Object value = request.getAttribute(TraceIdFilter.TRACE_ID_ATTRIBUTE);
    if (value instanceof String traceId && !traceId.isBlank()) {
      return traceId;
    }
    return "missing-trace-id";
  }
}
