package com.njxs.lims.masterdata.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record InternalUserCreateRequest(
    @NotBlank String username,
    @NotBlank String displayName,
    String email,
    UUID organizationUnitId,
    @NotBlank @Size(min = 8) String password,
    @NotEmpty List<String> roleCodes
) {
}
