package com.njxs.lims.device.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record DeviceBookingOverrideRequest(
    @NotBlank String reason,
    @NotNull Instant replacementStartAt,
    @NotNull Instant replacementEndAt
) {}
