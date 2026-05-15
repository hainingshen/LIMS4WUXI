package com.njxs.lims.contract.api;

import com.njxs.lims.audit.Audited;
import com.njxs.lims.auth.domain.AuthPrincipal;
import com.njxs.lims.auth.domain.ClientType;
import com.njxs.lims.auth.service.AuthContext;
import com.njxs.lims.common.api.PageResponse;
import com.njxs.lims.contract.persistence.ContractRepository;
import com.njxs.lims.contract.service.ContractService;
import com.njxs.lims.permission.RequiresAction;
import jakarta.validation.Valid;
import java.util.List;
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
@RequestMapping("/contracts")
@ConditionalOnProperty(prefix = "lims.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ContractController {

  private final ContractRepository contractRepository;
  private final ContractService contractService;

  public ContractController(ContractRepository contractRepository, ContractService contractService) {
    this.contractRepository = contractRepository;
    this.contractService = contractService;
  }

  @GetMapping
  public PageResponse<ContractResponse> list(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int pageSize
  ) {
    int normalizedPage = Math.max(page, 1);
    int normalizedPageSize = Math.min(Math.max(pageSize, 1), 100);
    int offset = (normalizedPage - 1) * normalizedPageSize;
    AuthPrincipal principal = AuthContext.require();
    if (principal.clientType() == ClientType.PORTAL) {
      return listPortalContracts(principal, normalizedPage, normalizedPageSize, offset);
    }
    return new PageResponse<>(
        contractRepository.list(normalizedPageSize, offset).stream().map(ContractResponse::from).toList(),
        normalizedPage,
        normalizedPageSize,
        contractRepository.count()
    );
  }

  private PageResponse<ContractResponse> listPortalContracts(
      AuthPrincipal principal,
      int normalizedPage,
      int normalizedPageSize,
      int offset
  ) {
    if (hasRole(principal, "CUSTOMER_APPLICANT")) {
      return new PageResponse<>(
          contractRepository.listForApplicant(principal.userId(), normalizedPageSize, offset)
              .stream()
              .map(ContractResponse::from)
              .toList(),
          normalizedPage,
          normalizedPageSize,
          contractRepository.countForApplicant(principal.userId())
      );
    }
    if (principal.orgId() != null
        && (hasRole(principal, "CUSTOMER_ORG_ADMIN") || hasRole(principal, "CUSTOMER_VIEWER"))) {
      return new PageResponse<>(
          contractRepository.listForCustomerOrg(principal.orgId(), normalizedPageSize, offset)
              .stream()
              .map(ContractResponse::from)
              .toList(),
          normalizedPage,
          normalizedPageSize,
          contractRepository.countForCustomerOrg(principal.orgId())
      );
    }
    return new PageResponse<>(List.of(), normalizedPage, normalizedPageSize, 0);
  }

  private boolean hasRole(AuthPrincipal principal, String roleCode) {
    return principal.roles().contains(roleCode);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Audited(actionCode = "CONTRACT_CREATE", entityType = "contract")
  @RequiresAction("CONTRACT_CREATE")
  public ContractResponse create(@Valid @RequestBody ContractCreateRequest request) {
    return contractService.create(request, AuthContext.require());
  }

  @PostMapping("/{id}/mark-signed")
  @Audited(actionCode = "CONTRACT_SIGN_CONFIRM", entityType = "contract")
  @RequiresAction("CONTRACT_SIGN_CONFIRM")
  public ContractResponse markSigned(
      @PathVariable UUID id,
      @RequestBody(required = false) ContractMarkSignedRequest request
  ) {
    return contractService.markSigned(id, request, AuthContext.require());
  }
}
