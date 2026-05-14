package com.njxs.lims.report.service;

import com.njxs.lims.auth.domain.AuthPrincipal;
import com.njxs.lims.permission.DataScopeEvaluator;
import com.njxs.lims.report.api.ReportGenerateDraftRequest;
import com.njxs.lims.report.persistence.ReportRepository;
import java.util.Arrays;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "lims.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ReportDataScopeEvaluator implements DataScopeEvaluator {

  private final ReportRepository reportRepository;

  public ReportDataScopeEvaluator(ReportRepository reportRepository) {
    this.reportRepository = reportRepository;
  }

  @Override
  public boolean supports(String actionCode, String scope) {
    return "REPORT_DRAFT_CREATE".equals(actionCode) && "COMPLETED_WORK_ORDERS".equals(scope);
  }

  @Override
  public boolean isAllowed(AuthPrincipal principal, String actionCode, String scope, Object[] args) {
    return request(args)
        .filter(request -> request.projectId() != null)
        .filter(request -> request.workOrderIds() != null && !request.workOrderIds().isEmpty())
        .map(request -> reportRepository.workOrdersBelongToProject(request.projectId(), request.workOrderIds()))
        .orElse(false);
  }

  private Optional<ReportGenerateDraftRequest> request(Object[] args) {
    return Arrays.stream(args)
        .filter(ReportGenerateDraftRequest.class::isInstance)
        .map(ReportGenerateDraftRequest.class::cast)
        .findFirst();
  }
}
