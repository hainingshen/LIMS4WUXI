package com.njxs.lims.sample.service;

import com.njxs.lims.auth.domain.AuthPrincipal;
import com.njxs.lims.common.exception.BusinessException;
import com.njxs.lims.project.domain.Project;
import com.njxs.lims.project.domain.ProjectStatus;
import com.njxs.lims.project.persistence.ProjectRepository;
import com.njxs.lims.project.service.ProjectStatusTransitionService;
import com.njxs.lims.sample.api.SampleReceiveRequest;
import com.njxs.lims.sample.api.SampleResponse;
import com.njxs.lims.sample.api.SampleStockInRequest;
import com.njxs.lims.sample.domain.Sample;
import com.njxs.lims.sample.domain.SampleStatus;
import com.njxs.lims.sample.persistence.SampleRepository;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(prefix = "lims.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SampleWorkflowService {

  private final SampleRepository sampleRepository;
  private final ProjectRepository projectRepository;
  private final ProjectStatusTransitionService transitionService;

  public SampleWorkflowService(
      SampleRepository sampleRepository,
      ProjectRepository projectRepository,
      ProjectStatusTransitionService transitionService
  ) {
    this.sampleRepository = sampleRepository;
    this.projectRepository = projectRepository;
    this.transitionService = transitionService;
  }

  @Transactional
  public SampleResponse receive(UUID id, SampleReceiveRequest request, AuthPrincipal principal) {
    Sample sample = sample(id);
    SampleStatus status = SampleStatus.valueOf(sample.getStatus());
    if (status != SampleStatus.WAITING_DELIVERY
        && status != SampleStatus.ARRIVED_PENDING_CHECK
        && status != SampleStatus.CHECK_EXCEPTION) {
      throw statusConflict("WAITING_DELIVERY|ARRIVED_PENDING_CHECK|CHECK_EXCEPTION", sample.getStatus());
    }

    sample.setReceivedAt(request.receivedAt());
    sample.setReceivedBy(request.receivedBy());
    sample.setRecipientName(principal.userId().toString());
    sample.setReceivedDate(LocalDate.ofInstant(request.receivedAt(), ZoneOffset.UTC));
    sample.setStatus(request.checkPassed() ? SampleStatus.PRE_IN_STOCK.name() : SampleStatus.CHECK_EXCEPTION.name());
    return SampleResponse.from(sampleRepository.updateWorkflow(sample, principal.userId()));
  }

  @Transactional
  public SampleResponse stockIn(UUID id, SampleStockInRequest request, AuthPrincipal principal) {
    Sample sample = sample(id);
    if (!SampleStatus.PRE_IN_STOCK.name().equals(sample.getStatus())) {
      throw statusConflict(SampleStatus.PRE_IN_STOCK.name(), sample.getStatus());
    }

    sample.setInventoryLocation(location(request));
    sample.setStatus(SampleStatus.IN_STOCK.name());
    Sample updated = sampleRepository.updateWorkflow(sample, principal.userId());
    markProjectSampleReceived(updated, principal.userId());
    return SampleResponse.from(updated);
  }

  private void markProjectSampleReceived(Sample sample, UUID actorUserId) {
    if (!sampleRepository.allProjectSamplesInStock(sample.getProjectId())) {
      return;
    }
    Project project = projectRepository.findById(sample.getProjectId()).orElse(null);
    if (project == null) {
      return;
    }
    if (ProjectStatus.CONTRACTED.name().equals(project.getStatus())
        || ProjectStatus.WAITING_SAMPLE.name().equals(project.getStatus())) {
      transitionService.transition(project, ProjectStatus.SAMPLE_RECEIVED, "all samples in stock");
      projectRepository.updateStatus(project, actorUserId);
    }
  }

  private Sample sample(UUID id) {
    return sampleRepository.findById(id)
        .orElseThrow(() -> new BusinessException(
            "NOT_FOUND",
            "资源不存在",
            HttpStatus.NOT_FOUND,
            Map.of("entityType", "sample", "entityId", id)
        ));
  }

  private BusinessException statusConflict(String expected, String actual) {
    return new BusinessException(
        "SAMPLE_STATUS_CONFLICT",
        "样品当前状态不允许该操作",
        HttpStatus.CONFLICT,
        Map.of("expected", expected, "actual", actual)
    );
  }

  private String location(SampleStockInRequest request) {
    if (request == null || request.inventoryLocation() == null || request.inventoryLocation().isBlank()) {
      return "UNASSIGNED";
    }
    return request.inventoryLocation();
  }
}
