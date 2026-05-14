package com.njxs.lims.report.delivery.api;

import com.njxs.lims.common.api.PageResponse;
import com.njxs.lims.permission.RequiresAction;
import com.njxs.lims.report.delivery.service.ReportDeliveryLogService;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/report-delivery-logs")
@ConditionalOnProperty(prefix = "lims.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ReportDeliveryLogController {

  private final ReportDeliveryLogService reportDeliveryLogService;

  public ReportDeliveryLogController(ReportDeliveryLogService reportDeliveryLogService) {
    this.reportDeliveryLogService = reportDeliveryLogService;
  }

  @GetMapping
  @RequiresAction("REPORT_REGISTRY_READ")
  public PageResponse<ReportDeliveryLogResponse> list(
      @RequestParam(required = false) UUID reportId,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int pageSize
  ) {
    return reportDeliveryLogService.list(reportId, page, pageSize);
  }
}
