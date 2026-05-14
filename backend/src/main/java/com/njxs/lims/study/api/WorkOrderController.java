package com.njxs.lims.study.api;

import com.njxs.lims.audit.Audited;
import com.njxs.lims.auth.service.AuthContext;
import com.njxs.lims.common.api.PageResponse;
import com.njxs.lims.permission.RequiresAction;
import com.njxs.lims.study.service.RawRecordService;
import com.njxs.lims.study.service.WorkOrderService;
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
@RequestMapping("/work-orders")
@ConditionalOnProperty(prefix = "lims.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class WorkOrderController {

  private final WorkOrderService workOrderService;
  private final RawRecordService rawRecordService;

  public WorkOrderController(WorkOrderService workOrderService, RawRecordService rawRecordService) {
    this.workOrderService = workOrderService;
    this.rawRecordService = rawRecordService;
  }

  @GetMapping
  public PageResponse<WorkOrderResponse> list(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int pageSize
  ) {
    return workOrderService.list(page, pageSize);
  }

  @PostMapping("/{id}/pick-sample")
  @Audited(actionCode = "WORK_ORDER_EXECUTE", entityType = "work_order")
  @RequiresAction("WORK_ORDER_EXECUTE")
  public WorkOrderResponse pickSample(@PathVariable UUID id) {
    return workOrderService.pickSample(id, AuthContext.require());
  }

  @PostMapping("/{id}/start")
  @Audited(actionCode = "WORK_ORDER_EXECUTE", entityType = "work_order")
  @RequiresAction("WORK_ORDER_EXECUTE")
  public WorkOrderResponse start(@PathVariable UUID id) {
    return workOrderService.start(id, AuthContext.require());
  }

  @PostMapping("/{id}/enter-result")
  @Audited(actionCode = "WORK_ORDER_EXECUTE", entityType = "work_order")
  @RequiresAction("WORK_ORDER_EXECUTE")
  public WorkOrderResponse enterResult(@PathVariable UUID id) {
    return workOrderService.enterResult(id, AuthContext.require());
  }

  @PostMapping("/{id}/submit-raw-record")
  @Audited(actionCode = "RAW_RECORD_SUBMIT", entityType = "work_order")
  @RequiresAction("RAW_RECORD_SUBMIT")
  public RawRecordResponse submitRawRecord(
      @PathVariable UUID id,
      @Valid @RequestBody RawRecordSubmitRequest request
  ) {
    return rawRecordService.submit(id, request, AuthContext.require());
  }
}
