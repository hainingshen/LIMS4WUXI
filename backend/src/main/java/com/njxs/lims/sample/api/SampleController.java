package com.njxs.lims.sample.api;

import com.njxs.lims.audit.Audited;
import com.njxs.lims.auth.service.AuthContext;
import com.njxs.lims.permission.RequiresAction;
import com.njxs.lims.sample.service.SampleWorkflowService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/samples")
@ConditionalOnProperty(prefix = "lims.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SampleController {

  private final SampleWorkflowService sampleWorkflowService;

  public SampleController(SampleWorkflowService sampleWorkflowService) {
    this.sampleWorkflowService = sampleWorkflowService;
  }

  @PostMapping("/{id}/receive")
  @Audited(actionCode = "SAMPLE_RECEIVE", entityType = "sample")
  @RequiresAction("SAMPLE_RECEIVE")
  public SampleResponse receive(@PathVariable UUID id, @Valid @RequestBody SampleReceiveRequest request) {
    return sampleWorkflowService.receive(id, request, AuthContext.require());
  }

  @PostMapping("/{id}/stock-in")
  @Audited(actionCode = "SAMPLE_STOCK_IN", entityType = "sample")
  @RequiresAction("SAMPLE_STOCK_IN")
  public SampleResponse stockIn(
      @PathVariable UUID id,
      @RequestBody(required = false) SampleStockInRequest request
  ) {
    return sampleWorkflowService.stockIn(id, request, AuthContext.require());
  }
}
