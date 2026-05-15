package com.njxs.lims.customer.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record PortalUserCreateRequest(
    @NotNull UUID customerOrgId,
    @NotBlank String username,
    @NotBlank String displayName,
    String email,
    @NotNull List<String> roleCodes
) {
}
