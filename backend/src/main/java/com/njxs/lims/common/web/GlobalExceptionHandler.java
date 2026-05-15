package com.njxs.lims.common.web;

import com.njxs.lims.auth.service.AuthenticationFailedException;
import com.njxs.lims.common.api.ApiError;
import com.njxs.lims.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

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

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiError> handleHttpMessageNotReadable(
      HttpMessageNotReadableException exception,
      HttpServletRequest request
  ) {
    return ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .body(error("INVALID_REQUEST_BODY", "请求体格式不合法", request, Map.of()));
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ApiError> handleMethodNotSupported(
      HttpRequestMethodNotSupportedException exception,
      HttpServletRequest request
  ) {
    return ResponseEntity
        .status(HttpStatus.METHOD_NOT_ALLOWED)
        .body(error(
            "METHOD_NOT_ALLOWED",
            "请求方法不支持",
            request,
            Map.of("method", request.getMethod())
        ));
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  public ResponseEntity<ApiError> handleMediaTypeNotSupported(
      HttpMediaTypeNotSupportedException exception,
      HttpServletRequest request
  ) {
    return ResponseEntity
        .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
        .body(error("UNSUPPORTED_MEDIA_TYPE", "请求内容类型不支持", request, Map.of()));
  }

  @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
  public ResponseEntity<ApiError> handleMediaTypeNotAcceptable(
      HttpMediaTypeNotAcceptableException exception,
      HttpServletRequest request
  ) {
    return ResponseEntity
        .status(HttpStatus.NOT_ACCEPTABLE)
        .body(error("NOT_ACCEPTABLE", "响应内容类型不支持", request, Map.of()));
  }

  @ExceptionHandler({
      MissingServletRequestParameterException.class,
      MissingServletRequestPartException.class,
      MissingPathVariableException.class,
      MethodArgumentTypeMismatchException.class,
      MaxUploadSizeExceededException.class
  })
  public ResponseEntity<ApiError> handleBadRequest(Exception exception, HttpServletRequest request) {
    return ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .body(error("VALIDATION_FAILED", "请求参数校验失败", request, Map.of("error", exception.getMessage())));
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
    log.error("Unhandled API exception for {} {}", request.getMethod(), request.getRequestURI(), exception);
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
