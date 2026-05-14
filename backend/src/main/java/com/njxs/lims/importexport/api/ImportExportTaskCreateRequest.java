package com.njxs.lims.importexport.api;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import java.util.UUID;

public record ImportExportTaskCreateRequest(
    @NotBlank String taskType,
    @NotBlank String entityType,
    UUID sourceFileId,
    Map<String, Object> parameters
) {
}
