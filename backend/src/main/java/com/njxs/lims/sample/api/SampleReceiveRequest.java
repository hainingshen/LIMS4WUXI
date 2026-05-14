package com.njxs.lims.sample.api;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record SampleReceiveRequest(
    @NotNull Instant receivedAt,
    @NotNull UUID receivedBy,
    @NotNull Boolean checkPassed,
    String exceptionNote
) {
}
