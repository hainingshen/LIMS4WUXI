package com.njxs.lims.study.service;

import com.njxs.lims.auth.domain.AuthPrincipal;
import com.njxs.lims.common.api.PageResponse;
import com.njxs.lims.common.exception.BusinessException;
import com.njxs.lims.study.api.WorkOrderResponse;
import com.njxs.lims.study.domain.WorkOrderRecord;
import com.njxs.lims.study.persistence.WorkOrderRepository;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(prefix = "lims.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class WorkOrderService {

  private final WorkOrderRepository workOrderRepository;

  public WorkOrderService(WorkOrderRepository workOrderRepository) {
    this.workOrderRepository = workOrderRepository;
  }

  public PageResponse<WorkOrderResponse> list(int page, int pageSize) {
    int normalizedPage = Math.max(page, 1);
    int normalizedPageSize = Math.min(Math.max(pageSize, 1), 100);
    int offset = (normalizedPage - 1) * normalizedPageSize;
    return new PageResponse<>(
        workOrderRepository.list(normalizedPageSize, offset).stream()
            .map(WorkOrderResponse::from)
            .toList(),
        normalizedPage,
        normalizedPageSize,
        workOrderRepository.count()
    );
  }

  @Transactional
  public WorkOrderResponse pickSample(UUID id, AuthPrincipal principal) {
    WorkOrderRecord record = workOrder(id);
    if (!"PENDING_EXECUTION".equals(record.status())) {
      throw statusConflict(Set.of("PENDING_EXECUTION"), record.status());
    }
    return WorkOrderResponse.from(workOrderRepository.updateStatus(id, "SAMPLE_PICKED", 5, principal.userId()));
  }

  @Transactional
  public WorkOrderResponse start(UUID id, AuthPrincipal principal) {
    WorkOrderRecord record = workOrder(id);
    if (!"SAMPLE_PICKED".equals(record.status())) {
      throw statusConflict(Set.of("SAMPLE_PICKED"), record.status());
    }
    return WorkOrderResponse.from(workOrderRepository.updateStatus(id, "IN_PROGRESS", 10, principal.userId()));
  }

  @Transactional
  public WorkOrderResponse enterResult(UUID id, AuthPrincipal principal) {
    WorkOrderRecord record = workOrder(id);
    if (!"IN_PROGRESS".equals(record.status())) {
      throw statusConflict(Set.of("IN_PROGRESS"), record.status());
    }
    return WorkOrderResponse.from(workOrderRepository.updateStatus(id, "RESULT_ENTERED", 80, principal.userId()));
  }

  private WorkOrderRecord workOrder(UUID id) {
    return workOrderRepository.findById(id)
        .orElseThrow(() -> new BusinessException(
            "NOT_FOUND",
            "资源不存在",
            HttpStatus.NOT_FOUND,
            Map.of("entityType", "work_order", "entityId", id)
        ));
  }

  private BusinessException statusConflict(Set<String> expected, String actual) {
    return new BusinessException(
        "WORK_ORDER_STATUS_CONFLICT",
        "工单当前状态不可执行该操作",
        HttpStatus.CONFLICT,
        Map.of("expected", expected, "actual", actual)
    );
  }
}
