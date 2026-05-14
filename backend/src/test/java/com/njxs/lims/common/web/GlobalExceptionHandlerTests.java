package com.njxs.lims.common.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.njxs.lims.LimsApplication;
import com.njxs.lims.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest(
    classes = {LimsApplication.class, GlobalExceptionHandlerTests.BusinessErrorController.class},
    properties = {
        "spring.autoconfigure.exclude="
            + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
            + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration",
        "lims.auth.enabled=false"
    }
)
@AutoConfigureMockMvc
class GlobalExceptionHandlerTests {

  private final MockMvc mockMvc;

  @Autowired
  GlobalExceptionHandlerTests(MockMvc mockMvc) {
    this.mockMvc = mockMvc;
  }

  @Test
  void businessExceptionReturnsContractedErrorResponse() throws Exception {
    mockMvc.perform(get("/test/business-error").header(TraceIdFilter.TRACE_ID_HEADER, "trace-test-001"))
        .andExpect(status().isLocked())
        .andExpect(header().string(TraceIdFilter.TRACE_ID_HEADER, "trace-test-001"))
        .andExpect(jsonPath("$.code").value("PAYMENT_GATE_LOCKED"))
        .andExpect(jsonPath("$.message").value("未到账，禁止下载正式报告"))
        .andExpect(jsonPath("$.traceId").value("trace-test-001"))
        .andExpect(jsonPath("$.details").isMap());
  }

  @Test
  void missingResourceReturnsContractedErrorResponse() throws Exception {
    mockMvc.perform(get("/test/missing").header(TraceIdFilter.TRACE_ID_HEADER, "trace-test-404"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"))
        .andExpect(jsonPath("$.message").value("资源不存在"))
        .andExpect(jsonPath("$.traceId").value("trace-test-404"))
        .andExpect(jsonPath("$.details.path").value("/test/missing"));
  }

  @RestController
  public static class BusinessErrorController {

    @GetMapping("/test/business-error")
    void throwBusinessError() {
      throw new BusinessException("PAYMENT_GATE_LOCKED");
    }
  }
}
