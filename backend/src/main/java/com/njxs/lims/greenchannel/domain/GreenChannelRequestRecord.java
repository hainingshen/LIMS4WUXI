package com.njxs.lims.greenchannel.domain;

import java.time.Instant;
import java.util.UUID;

public record GreenChannelRequestRecord(
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
}
