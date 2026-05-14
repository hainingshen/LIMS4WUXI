package com.njxs.lims.contract.api;

import com.njxs.lims.contract.domain.Contract;
import java.time.Instant;
import java.util.UUID;

public record ContractResponse(
    UUID id,
    String bizNo,
    UUID projectId,
    UUID customerOrgId,
    String contractType,
    String templateCode,
    String status,
    Instant signedAt,
    UUID signedFileId
) {

  public static ContractResponse from(Contract contract) {
    return new ContractResponse(
        contract.getId(),
        contract.getBizNo(),
        contract.getProjectId(),
        contract.getCustomerOrgId(),
        contract.getContractType(),
        contract.getTemplateCode(),
        contract.getStatus(),
        contract.getSignedAt(),
        contract.getSignedFileId()
    );
  }
}
