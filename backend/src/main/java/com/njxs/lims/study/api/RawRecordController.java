package com.njxs.lims.study.api;

import com.njxs.lims.audit.Audited;
import com.njxs.lims.auth.domain.ClientType;
import com.njxs.lims.auth.service.AuthContext;
import com.njxs.lims.common.api.PageResponse;
import com.njxs.lims.permission.RequiresAction;
import com.njxs.lims.permission.RequiresClient;
import com.njxs.lims.study.service.RawRecordService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/raw-records")
@ConditionalOnProperty(prefix = "lims.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RawRecordController {

  private final RawRecordService rawRecordService;

  public RawRecordController(RawRecordService rawRecordService) {
    this.rawRecordService = rawRecordService;
  }

  @GetMapping
  @RequiresClient(ClientType.INTERNAL)
  public PageResponse<RawRecordResponse> list(
      @RequestParam(required = false) UUID workOrderId,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int pageSize
  ) {
    return rawRecordService.list(AuthContext.require(), workOrderId, page, pageSize);
  }

  @PostMapping("/{id}/review")
  @Audited(actionCode = "RAW_RECORD_REVIEW", entityType = "raw_record")
  @RequiresAction("RAW_RECORD_REVIEW")
  public RawRecordResponse review(
      @PathVariable UUID id,
      @Valid @RequestBody RawRecordReviewRequest request
  ) {
    return rawRecordService.review(id, request, AuthContext.require());
  }
}
