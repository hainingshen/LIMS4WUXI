package com.njxs.lims.contract.service;

import com.njxs.lims.auth.domain.AuthPrincipal;
import com.njxs.lims.contract.api.ContractCreateRequest;
import com.njxs.lims.contract.persistence.ContractRepository;
import com.njxs.lims.permission.DataScopeEvaluator;
import com.njxs.lims.project.persistence.ProjectRepository;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "lims.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ContractDataScopeEvaluator implements DataScopeEvaluator {

  private static final String ACTION_CONTRACT_CREATE = "CONTRACT_CREATE";
  private static final String ACTION_CONTRACT_SIGN_CONFIRM = "CONTRACT_SIGN_CONFIRM";

  private final ContractRepository contractRepository;
  private final ProjectRepository projectRepository;

  public ContractDataScopeEvaluator(ContractRepository contractRepository, ProjectRepository projectRepository) {
    this.contractRepository = contractRepository;
    this.projectRepository = projectRepository;
  }

  @Override
  public boolean supports(String actionCode, String scope) {
    return (ACTION_CONTRACT_CREATE.equals(actionCode) && "ASSIGNED_PROJECT".equals(scope))
        || (ACTION_CONTRACT_SIGN_CONFIRM.equals(actionCode)
            && ("ASSIGNED_PROJECT".equals(scope) || "OWN".equals(scope) || "ORG".equals(scope)));
  }

  @Override
  public boolean isAllowed(AuthPrincipal principal, String actionCode, String scope, Object[] args) {
    if (ACTION_CONTRACT_CREATE.equals(actionCode)) {
      return projectId(args)
          .map(projectId -> projectRepository.existsByAssignedSales(projectId, principal.userId()))
          .orElse(false);
    }

    Optional<UUID> contractId = firstUuid(args);
    if (contractId.isEmpty()) {
      return false;
    }
    if ("ASSIGNED_PROJECT".equals(scope)) {
      return contractRepository.existsByAssignedSales(contractId.get(), principal.userId());
    }
    if ("OWN".equals(scope)) {
      return contractRepository.existsByApplicant(contractId.get(), principal.userId());
    }
    return principal.orgId() != null && contractRepository.existsByCustomerOrg(contractId.get(), principal.orgId());
  }

  private Optional<UUID> projectId(Object[] args) {
    return Arrays.stream(args)
        .filter(ContractCreateRequest.class::isInstance)
        .map(ContractCreateRequest.class::cast)
        .map(ContractCreateRequest::projectId)
        .findFirst();
  }

  private Optional<UUID> firstUuid(Object[] args) {
    return Arrays.stream(args)
        .filter(UUID.class::isInstance)
        .map(UUID.class::cast)
        .findFirst();
  }
}
