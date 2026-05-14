package com.njxs.lims.report.service;

import com.njxs.lims.auth.domain.AuthPrincipal;
import com.njxs.lims.permission.DataScopeEvaluator;
import com.njxs.lims.report.api.ReportGenerateDraftRequest;
import com.njxs.lims.report.persistence.ReportRepository;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "lims.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ReportDataScopeEvaluator implements DataScopeEvaluator {

  private static final Set<String> TASK_ACTIONS = Set.of(
      "REPORT_QC_REVIEW",
      "REPORT_QA_REVIEW",
      "REPORT_SD_REVIEW",
      "REPORT_ISSUE"
  );

  private final ReportRepository reportRepository;

  public ReportDataScopeEvaluator(ReportRepository reportRepository) {
    this.reportRepository = reportRepository;
  }

  @Override
  public boolean supports(String actionCode, String scope) {
    return ("REPORT_DRAFT_CREATE".equals(actionCode) && "COMPLETED_WORK_ORDERS".equals(scope))
        || (TASK_ACTIONS.contains(actionCode) && "TASK".equals(scope));
  }

  @Override
  public boolean isAllowed(AuthPrincipal principal, String actionCode, String scope, Object[] args) {
    if (TASK_ACTIONS.contains(actionCode)) {
      return firstUuid(args).map(reportRepository::existsById).orElse(false);
    }
    return request(args)
        .filter(request -> request.projectId() != null)
        .filter(request -> request.workOrderIds() != null && !request.workOrderIds().isEmpty())
        .map(request -> reportRepository.workOrdersBelongToProject(request.projectId(), request.workOrderIds()))
        .or(() -> firstUuid(args).map(reportRepository::existsById))
        .orElse(false);
  }

  private Optional<ReportGenerateDraftRequest> request(Object[] args) {
    return Arrays.stream(args)
        .filter(ReportGenerateDraftRequest.class::isInstance)
        .map(ReportGenerateDraftRequest.class::cast)
        .findFirst();
  }

  private Optional<UUID> firstUuid(Object[] args) {
    return Arrays.stream(args)
        .filter(UUID.class::isInstance)
        .map(UUID.class::cast)
        .findFirst();
  }
}
