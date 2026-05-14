package com.njxs.lims.report.api;

import com.njxs.lims.audit.Audited;
import com.njxs.lims.auth.service.AuthContext;
import com.njxs.lims.common.api.PageResponse;
import com.njxs.lims.permission.RequiresAction;
import com.njxs.lims.report.service.ReportService;
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
@RequestMapping("/reports")
@ConditionalOnProperty(prefix = "lims.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ReportController {

  private final ReportService reportService;

  public ReportController(ReportService reportService) {
    this.reportService = reportService;
  }

  @GetMapping
  public PageResponse<ReportResponse> list(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int pageSize
  ) {
    return reportService.list(page, pageSize);
  }

  @PostMapping("/generate-draft")
  @ResponseStatus(HttpStatus.CREATED)
  @Audited(actionCode = "REPORT_DRAFT_CREATE", entityType = "report")
  @RequiresAction("REPORT_DRAFT_CREATE")
  public ReportResponse generateDraft(@Valid @RequestBody ReportGenerateDraftRequest request) {
    return reportService.generateDraft(request, AuthContext.require());
  }

  @PostMapping("/{id}/submit-qc")
  @Audited(actionCode = "REPORT_DRAFT_SUBMIT_QC", entityType = "report")
  @RequiresAction("REPORT_DRAFT_CREATE")
  public ReportResponse submitQc(@PathVariable UUID id) {
    return reportService.submitQc(id, AuthContext.require());
  }

  @PostMapping("/{id}/issue")
  @Audited(actionCode = "REPORT_ISSUE", entityType = "report")
  @RequiresAction("REPORT_ISSUE")
  public ReportResponse issue(@PathVariable UUID id) {
    return reportService.issue(id, AuthContext.require());
  }

  @PostMapping("/{reportId}/review")
  @Audited(actionCode = "REPORT_REVIEW", entityType = "report")
  public ReportResponse review(
      @PathVariable UUID reportId,
      @Valid @RequestBody ReportReviewRequest request
  ) {
    return reportService.review(reportId, request, AuthContext.require());
  }
}
