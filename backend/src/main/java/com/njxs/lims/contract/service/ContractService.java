package com.njxs.lims.contract.service;

import com.njxs.lims.auth.domain.AuthPrincipal;
import com.njxs.lims.common.exception.BusinessException;
import com.njxs.lims.contract.api.ContractCreateRequest;
import com.njxs.lims.contract.api.ContractMarkSignedRequest;
import com.njxs.lims.contract.api.ContractResponse;
import com.njxs.lims.contract.domain.Contract;
import com.njxs.lims.contract.domain.ContractStatus;
import com.njxs.lims.contract.domain.ContractType;
import com.njxs.lims.contract.persistence.ContractRepository;
import com.njxs.lims.project.domain.Project;
import com.njxs.lims.project.domain.ProjectStatus;
import com.njxs.lims.project.persistence.ProjectRepository;
import com.njxs.lims.project.service.ProjectStatusTransitionService;
import com.njxs.lims.submission.persistence.SubmissionRepository;
import java.time.Instant;
import java.time.Year;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(prefix = "lims.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ContractService {

  private final ContractRepository contractRepository;
  private final ProjectRepository projectRepository;
  private final ProjectStatusTransitionService transitionService;
  private final SubmissionRepository submissionRepository;

  public ContractService(
      ContractRepository contractRepository,
      ProjectRepository projectRepository,
      ProjectStatusTransitionService transitionService,
      SubmissionRepository submissionRepository
  ) {
    this.contractRepository = contractRepository;
    this.projectRepository = projectRepository;
    this.transitionService = transitionService;
    this.submissionRepository = submissionRepository;
  }

  @Transactional
  public ContractResponse create(ContractCreateRequest request, AuthPrincipal principal) {
    Project project = project(request.projectId());
    assertContractCreatable(project);

    Instant now = Instant.now();
    Contract contract = new Contract();
    contract.setId(UUID.randomUUID());
    contract.setBizNo(contractRepository.nextContractBizNo(Year.now().getValue()));
    contract.setProjectId(project.getId());
    contract.setCustomerOrgId(project.getCustomerOrgId());
    contract.setContractType(contractType(request.contractType()));
    contract.setTemplateCode(request.templateCode());
    contract.setStatus(ContractStatus.PENDING_SIGN.name());
    contract.setSourceFileId(request.fileId());
    contract.setCreatedAt(now);
    contract.setCreatedBy(principal.userId());
    contract.setUpdatedAt(now);
    contract.setUpdatedBy(principal.userId());
    contract.setTenantOrgId(project.getTenantOrgId());
    contract.setSourceType("NEW");
    contract.setDataQualityScore(100);
    contractRepository.save(contract);
    return ContractResponse.from(contract);
  }

  @Transactional
  public ContractResponse markSigned(UUID id, ContractMarkSignedRequest request, AuthPrincipal principal) {
    Contract contract = contract(id);
    if (ContractStatus.SIGNED.name().equals(contract.getStatus())) {
      return ContractResponse.from(contract);
    }
    if (ContractStatus.CANCELLED.name().equals(contract.getStatus())
        || ContractStatus.SUPERSEDED.name().equals(contract.getStatus())) {
      throw new BusinessException(
          "CONTRACT_STATUS_CONFLICT",
          "合同当前状态不可确认签署",
          HttpStatus.CONFLICT,
          Map.of("actual", contract.getStatus())
      );
    }

    Project project = project(contract.getProjectId());
    transitionService.transition(project, ProjectStatus.CONTRACTED, "contract signed");
    projectRepository.updateStatus(project, principal.userId());

    contract.setStatus(ContractStatus.SIGNED.name());
    contract.setSignedAt(signedAt(request));
    contract.setSignedFileId(request == null ? null : request.signedFileId());
    Contract signed = contractRepository.markSigned(contract, principal.userId());
    submissionRepository.markChangeLocked(project.getSubmissionId(), principal.userId());
    return ContractResponse.from(signed);
  }

  private void assertContractCreatable(Project project) {
    if (!ProjectStatus.QUOTED.name().equals(project.getStatus())) {
      throw new BusinessException(
          "PROJECT_NOT_CONTRACTABLE",
          "项目未达到可签约状态",
          HttpStatus.CONFLICT,
          Map.of("expected", ProjectStatus.QUOTED.name(), "actual", project.getStatus())
      );
    }
  }

  private String contractType(String value) {
    try {
      return ContractType.valueOf(value).name();
    } catch (IllegalArgumentException exception) {
      throw new BusinessException(
          "VALIDATION_FAILED",
          "请求参数校验失败",
          HttpStatus.BAD_REQUEST,
          Map.of("contractType", "Unsupported contract type: " + value)
      );
    }
  }

  private Contract contract(UUID id) {
    return contractRepository.findById(id)
        .orElseThrow(() -> new BusinessException(
            "NOT_FOUND",
            "资源不存在",
            HttpStatus.NOT_FOUND,
            Map.of("entityType", "contract", "entityId", id)
        ));
  }

  private Project project(UUID id) {
    return projectRepository.findById(id)
        .orElseThrow(() -> new BusinessException(
            "NOT_FOUND",
            "资源不存在",
            HttpStatus.NOT_FOUND,
            Map.of("entityType", "project", "entityId", id)
        ));
  }

  private Instant signedAt(ContractMarkSignedRequest request) {
    if (request == null || request.signedAt() == null) {
      return Instant.now();
    }
    return request.signedAt();
  }
}
