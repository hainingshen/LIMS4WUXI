package com.njxs.lims.masterdata.api;

import com.njxs.lims.masterdata.domain.HolidayRecord;
import java.time.LocalDate;
import java.util.UUID;

public record HolidayResponse(
    UUID id,
    LocalDate date,
    String type,
    String name
) {
  public static HolidayResponse from(HolidayRecord record) {
    return new HolidayResponse(record.id(), record.date(), record.type(), record.name());
  }
}
