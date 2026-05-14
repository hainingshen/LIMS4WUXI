package com.njxs.lims.sample.retention.api;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record SampleRetentionDisposeRequest(
    @NotBlank String disposition,
    @NotBlank String reason,
    UUID proofFileId
) {
}
