package com.njxs.lims.sample.retention.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record SampleRetentionExtendRequest(
    @NotNull LocalDate retainUntil,
    @NotBlank String reason
) {
}
