package com.njxs.lims.common.exception;

import org.springframework.http.HttpStatus;

final class BusinessErrorCatalog {

  private BusinessErrorCatalog() {
  }

  static String messageOf(String code) {
    return switch (code) {
      case "PAYMENT_GATE_LOCKED" -> "未到账，禁止下载正式报告";
      default -> code;
    };
  }

  static HttpStatus statusOf(String code) {
    return switch (code) {
      case "PAYMENT_GATE_LOCKED" -> HttpStatus.LOCKED;
      default -> HttpStatus.BAD_REQUEST;
    };
  }
}
