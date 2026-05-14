package com.njxs.lims.contract.api;

import com.njxs.lims.audit.Audited;
import com.njxs.lims.auth.service.AuthContext;
import com.njxs.lims.contract.service.ContractService;
import com.njxs.lims.permission.RequiresAction;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/contracts")
@ConditionalOnProperty(prefix = "lims.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ContractController {

  private final ContractService contractService;

  public ContractController(ContractService contractService) {
    this.contractService = contractService;
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
