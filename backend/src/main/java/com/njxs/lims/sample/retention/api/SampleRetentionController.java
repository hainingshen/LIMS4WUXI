package com.njxs.lims.sample.retention.api;

import com.njxs.lims.audit.Audited;
import com.njxs.lims.auth.domain.ClientType;
import com.njxs.lims.auth.service.AuthContext;
import com.njxs.lims.common.api.PageResponse;
import com.njxs.lims.permission.RequiresAction;
import com.njxs.lims.permission.RequiresClient;
import com.njxs.lims.sample.retention.service.SampleRetentionService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sample-retentions")
@ConditionalOnProperty(prefix = "lims.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SampleRetentionController {

  private final SampleRetentionService retentionService;

  public SampleRetentionController(SampleRetentionService retentionService) {
    this.retentionService = retentionService;
  }

  @GetMapping
  @RequiresClient(ClientType.INTERNAL)
  public PageResponse<SampleRetentionResponse> list(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int pageSize
  ) {
    return retentionService.list(page, pageSize);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Audited(actionCode = "SAMPLE_RETENTION_REGISTER", entityType = "sample_retention")
  @RequiresAction("SAMPLE_RETENTION_REGISTER")
  public SampleRetentionResponse create(@Valid @RequestBody SampleRetentionCreateRequest request) {
    return retentionService.create(request, AuthContext.require());
  }

  @PostMapping("/{id}/register")
  @Audited(actionCode = "SAMPLE_RETENTION_REGISTER", entityType = "sample_retention")
  @RequiresAction("SAMPLE_RETENTION_REGISTER")
  public SampleRetentionResponse register(@PathVariable UUID id) {
    return retentionService.register(id, AuthContext.require());
  }

  @PostMapping("/{id}/extend")
  @Audited(actionCode = "SAMPLE_RETENTION_EXTEND", entityType = "sample_retention")
  @RequiresAction("SAMPLE_RETENTION_EXTEND")
  public SampleRetentionResponse extend(
      @PathVariable UUID id,
      @Valid @RequestBody SampleRetentionExtendRequest request
  ) {
    return retentionService.extend(id, request, AuthContext.require());
  }

  @PostMapping("/{id}/dispose")
  @Audited(actionCode = "SAMPLE_RETENTION_DISPOSE", entityType = "sample_retention")
  @RequiresAction("SAMPLE_RETENTION_DISPOSE")
  public SampleRetentionResponse dispose(
      @PathVariable UUID id,
      @Valid @RequestBody SampleRetentionDisposeRequest request
  ) {
    return retentionService.dispose(id, request, AuthContext.require());
  }
}
