package com.njxs.lims.study.api;

import com.njxs.lims.study.domain.RawRecordRecord;
import java.util.UUID;

public record RawRecordResponse(
    UUID id,
    String bizNo,
    UUID workOrderId,
    UUID projectId,
    UUID sampleId,
    String templateCode,
    String testItem,
    String status,
    UUID testerId,
    UUID checkerId
) {

  public static RawRecordResponse from(RawRecordRecord record) {
    return new RawRecordResponse(
        record.id(),
        record.bizNo(),
        record.workOrderId(),
        record.projectId(),
        record.sampleId(),
        record.templateCode(),
        record.testItem(),
        record.status(),
        record.testerId(),
        record.checkerId()
    );
  }
}
