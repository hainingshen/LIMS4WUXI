package com.njxs.lims.change.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ChangeRequestRecord(
    UUID id,
    String bizNo,
    String targetType,
    UUID targetId,
    String targetBizNo,
    String changeType,
    String reason,
    String status,
    UUID requestedBy,
    Instant requestedAt,
    Instant effectiveAt,
    UUID attachmentFileId,
    List<ChangeRequestItemRecord> items
) {
}
