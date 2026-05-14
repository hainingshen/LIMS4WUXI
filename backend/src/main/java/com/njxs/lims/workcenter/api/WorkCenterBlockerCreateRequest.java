package com.njxs.lims.workcenter.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record WorkCenterBlockerCreateRequest(
    @NotNull UUID projectId,
    @NotBlank String blockerType,
    @NotBlank String severity,
    @NotBlank String description,
    @NotBlank String impactScope,
    @NotNull Instant expectedRecoveryAt,
    @NotNull UUID ownerUserId,
    List<UUID> collaboratorIds
) {
}
