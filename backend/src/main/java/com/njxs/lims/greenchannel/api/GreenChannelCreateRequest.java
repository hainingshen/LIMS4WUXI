package com.njxs.lims.greenchannel.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record GreenChannelCreateRequest(
    @NotNull UUID projectId,
    @NotNull UUID reportId,
    @NotBlank String reason
) {
}
