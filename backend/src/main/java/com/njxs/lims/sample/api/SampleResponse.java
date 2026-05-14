package com.njxs.lims.sample.api;

import com.njxs.lims.sample.domain.Sample;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record SampleResponse(
    UUID id,
    String bizNo,
    UUID projectId,
    String sampleNameZh,
    String sampleNameEn,
    String modelRef,
    String lotBatch,
    String physicalState,
    String sterilizationState,
    String storageCondition,
    String status,
    Instant receivedAt,
    UUID receivedBy,
    String recipientName,
    LocalDate receivedDate,
    String inventoryLocation
) {

  public static SampleResponse from(Sample sample) {
    return new SampleResponse(
        sample.getId(),
        sample.getBizNo(),
        sample.getProjectId(),
        sample.getSampleNameZh(),
        sample.getSampleNameEn(),
        sample.getModelRef(),
        sample.getLotBatch(),
        sample.getPhysicalState(),
        sample.getSterilizationState(),
        sample.getStorageCondition(),
        sample.getStatus(),
        sample.getReceivedAt(),
        sample.getReceivedBy(),
        sample.getRecipientName(),
        sample.getReceivedDate(),
        sample.getInventoryLocation()
    );
  }
}
