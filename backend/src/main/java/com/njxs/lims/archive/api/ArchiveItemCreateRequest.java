package com.njxs.lims.archive.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ArchiveItemCreateRequest(
    @NotNull UUID projectId,
    String entityType,
    UUID entityId,
    @NotBlank String archiveType,
    @NotNull UUID fileId
) {
}
