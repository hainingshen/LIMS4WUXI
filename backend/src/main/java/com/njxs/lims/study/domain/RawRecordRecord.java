package com.njxs.lims.study.domain;

import java.time.Instant;
import java.util.UUID;

public record RawRecordRecord(
    UUID id,
    String bizNo,
    UUID workOrderId,
    UUID projectId,
    UUID sampleId,
    String templateCode,
    String testItem,
    String testBasis,
    String testTime,
    String testLocation,
    String temperatureCelsius,
    String humidityPercent,
    String method,
    String recordData,
    String attachmentFileIds,
    String status,
    UUID testerId,
    UUID checkerId,
    Instant submittedAt,
    Instant reviewedAt
) {
}
