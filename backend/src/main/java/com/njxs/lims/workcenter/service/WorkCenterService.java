package com.njxs.lims.workcenter.service;

import com.njxs.lims.auth.domain.AuthPrincipal;
import com.njxs.lims.common.api.PageResponse;
import com.njxs.lims.workcenter.api.WorkCenterProjectResponse;
import com.njxs.lims.workcenter.api.WorkCenterResourceLoadResponse;
import com.njxs.lims.workcenter.api.WorkCenterSummaryResponse;
import com.njxs.lims.workcenter.api.WorkCenterTrendResponse;
import com.njxs.lims.workcenter.domain.WorkCenterScope;
import com.njxs.lims.workcenter.persistence.WorkCenterRepository;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "lims.persistence", name = "enabled", havingValue = "true", matchIfMissing = true)
public class WorkCenterService {

  private final WorkCenterRepository workCenterRepository;

  public WorkCenterService(WorkCenterRepository workCenterRepository) {
    this.workCenterRepository = workCenterRepository;
  }

  public WorkCenterSummaryResponse summary(AuthPrincipal principal) {
    return WorkCenterSummaryResponse.from(workCenterRepository.summary(WorkCenterScope.from(principal)));
  }

  public PageResponse<WorkCenterProjectResponse> projects(
      AuthPrincipal principal,
      String filter,
      int page,
      int pageSize
  ) {
    int normalizedPage = Math.max(page, 1);
    int normalizedPageSize = Math.min(Math.max(pageSize, 1), 100);
    int offset = (normalizedPage - 1) * normalizedPageSize;
    WorkCenterScope scope = WorkCenterScope.from(principal);
    return new PageResponse<>(
        workCenterRepository.projects(scope, filter, normalizedPageSize, offset).stream()
            .map(WorkCenterProjectResponse::from)
            .toList(),
        normalizedPage,
        normalizedPageSize,
        workCenterRepository.countProjects(scope, filter)
    );
  }

  public List<WorkCenterResourceLoadResponse> resourceLoad(AuthPrincipal principal) {
    return workCenterRepository.resourceLoad(WorkCenterScope.from(principal)).stream()
        .map(WorkCenterResourceLoadResponse::from)
        .toList();
  }

  public List<WorkCenterTrendResponse> trends(AuthPrincipal principal) {
    return workCenterRepository.trends(WorkCenterScope.from(principal)).stream()
        .map(WorkCenterTrendResponse::from)
        .toList();
  }
}
