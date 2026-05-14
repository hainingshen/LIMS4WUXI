package com.njxs.lims.contract.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ContractCreateRequest(
    @NotNull UUID projectId,
    @NotBlank String contractType,
    @NotBlank String templateCode,
    UUID fileId
) {
}
