package com.njxs.lims.report.template.api;

import com.njxs.lims.audit.Audited;
import com.njxs.lims.auth.domain.ClientType;
import com.njxs.lims.auth.service.AuthContext;
import com.njxs.lims.common.api.PageResponse;
import com.njxs.lims.permission.RequiresAction;
import com.njxs.lims.permission.RequiresClient;
import com.njxs.lims.report.template.service.ReportTemplateService;
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
@RequestMapping("/report-templates")
@ConditionalOnProperty(prefix = "lims.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ReportTemplateController {

  private final ReportTemplateService reportTemplateService;

  public ReportTemplateController(ReportTemplateService reportTemplateService) {
    this.reportTemplateService = reportTemplateService;
  }

  @GetMapping
  @RequiresClient(ClientType.INTERNAL)
  public PageResponse<ReportTemplateResponse> list(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int pageSize
  ) {
    return reportTemplateService.list(page, pageSize);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Audited(actionCode = "REPORT_TEMPLATE_UPLOAD", entityType = "report_template")
  @RequiresAction("REPORT_TEMPLATE_UPLOAD")
  public ReportTemplateResponse create(@Valid @RequestBody ReportTemplateCreateRequest request) {
    return reportTemplateService.create(request, AuthContext.require());
  }

  @PostMapping("/{id}/publish")
  @Audited(actionCode = "REPORT_TEMPLATE_PUBLISH", entityType = "report_template")
  @RequiresAction("REPORT_TEMPLATE_PUBLISH")
  public ReportTemplateResponse publish(@PathVariable UUID id) {
    return reportTemplateService.publish(id, AuthContext.require());
  }

  @PostMapping("/{id}/deprecate")
  @Audited(actionCode = "REPORT_TEMPLATE_DEPRECATE", entityType = "report_template")
  @RequiresAction("REPORT_TEMPLATE_DEPRECATE")
  public ReportTemplateResponse deprecate(@PathVariable UUID id) {
    return reportTemplateService.deprecate(id, AuthContext.require());
  }
}
