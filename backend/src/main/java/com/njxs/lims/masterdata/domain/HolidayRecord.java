package com.njxs.lims.masterdata.domain;

import java.time.LocalDate;
import java.util.UUID;

public record HolidayRecord(
    UUID id,
    LocalDate date,
    String type,
    String name
) {
}
