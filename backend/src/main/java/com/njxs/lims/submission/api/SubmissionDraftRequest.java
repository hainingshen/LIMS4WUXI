package com.njxs.lims.submission.api;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SubmissionDraftRequest(
    @NotBlank String formCode,
    @NotNull @Min(1) Integer schemaVersion,
    @NotNull JsonNode data
) {
}
