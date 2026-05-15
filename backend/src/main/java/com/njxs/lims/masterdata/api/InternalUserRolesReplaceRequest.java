package com.njxs.lims.masterdata.api;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record InternalUserRolesReplaceRequest(
    @NotEmpty List<String> roleCodes
) {
}
