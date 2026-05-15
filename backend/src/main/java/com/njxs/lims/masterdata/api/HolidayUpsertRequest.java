package com.njxs.lims.masterdata.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record HolidayUpsertRequest(
    UUID id,
    @NotNull LocalDate date,
    @NotBlank String type,
    @NotBlank String name
) {
}
