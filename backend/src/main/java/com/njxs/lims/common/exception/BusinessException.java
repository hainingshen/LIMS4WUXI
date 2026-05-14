package com.njxs.lims.common.exception;

import java.util.Map;
import org.springframework.http.HttpStatus;

public class BusinessException extends RuntimeException {

  private final String code;
  private final HttpStatus status;
  private final Map<String, Object> details;

  public BusinessException(String code) {
    this(code, BusinessErrorCatalog.messageOf(code), BusinessErrorCatalog.statusOf(code), Map.of());
  }

  public BusinessException(String code, Map<String, Object> details) {
    this(code, BusinessErrorCatalog.messageOf(code), BusinessErrorCatalog.statusOf(code), details);
  }

  public BusinessException(String code, String message, HttpStatus status, Map<String, Object> details) {
    super(message);
    this.code = code;
    this.status = status;
    this.details = details == null ? Map.of() : Map.copyOf(details);
  }

  public String getCode() {
    return code;
  }

  public HttpStatus getStatus() {
    return status;
  }

  public Map<String, Object> getDetails() {
    return details;
  }
}
