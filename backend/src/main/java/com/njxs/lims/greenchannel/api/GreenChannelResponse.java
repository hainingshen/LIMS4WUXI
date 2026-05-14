package com.njxs.lims.greenchannel.api;

import com.njxs.lims.greenchannel.domain.GreenChannelRequestRecord;
import java.time.Instant;
import java.util.UUID;

public record GreenChannelResponse(
    UUID id,
    String bizNo,
    UUID projectId,
    UUID reportId,
    String reason,
    String status,
    UUID requestedBy,
    Instant requestedAt,
    String unlockScope
) {

  public static GreenChannelResponse from(GreenChannelRequestRecord record) {
    return new GreenChannelResponse(
        record.id(),
        record.bizNo(),
        record.projectId(),
        record.reportId(),
        record.reason(),
        record.status(),
        record.requestedBy(),
        record.requestedAt(),
        record.unlockScope()
    );
  }
}
