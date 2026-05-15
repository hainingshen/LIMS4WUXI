package com.njxs.lims.device.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record DeviceBookingCreateRequest(
    @NotNull UUID deviceId,
    UUID workOrderId,
    @NotBlank String purpose,
    @NotNull Instant startAt,
    @NotNull Instant endAt
) {}
