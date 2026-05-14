package com.njxs.lims.study.api;

import jakarta.validation.constraints.NotBlank;

public record RawRecordReviewRequest(
    @NotBlank String stage,
    @NotBlank String outcome,
    String comment
) {
}
