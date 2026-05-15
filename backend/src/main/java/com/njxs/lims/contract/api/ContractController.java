package com.njxs.lims.contract.api;

import com.njxs.lims.audit.Audited;
import com.njxs.lims.auth.domain.ClientType;
import com.njxs.lims.auth.service.AuthContext;
import com.njxs.lims.common.api.PageResponse;
import com.njxs.lims.contract.persistence.ContractRepository;
import com.njxs.lims.contract.service.ContractService;
import com.njxs.lims.permission.RequiresAction;
import com.njxs.lims.permission.RequiresClient;
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
  @RequiresClient(ClientType.INTERNAL)
  public PageResponse<ContractResponse> list(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int pageSize
  ) {
    int normalizedPage = Math.max(page, 1);
    int normalizedPageSize = Math.min(Math.max(pageSize, 1), 100);
    int offset = (normalizedPage - 1) * normalizedPageSize;
    return new PageResponse<>(
        contractRepository.list(normalizedPageSize, offset).stream().map(ContractResponse::from).toList(),
        normalizedPage,
        normalizedPageSize,
        contractRepository.count()
    );
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
