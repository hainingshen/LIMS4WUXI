package com.njxs.lims.change.api;

import com.njxs.lims.change.domain.ChangeRequestRecord;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ChangeRequestResponse(
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
    List<ChangeRequestItemResponse> items
) {

  public static ChangeRequestResponse from(ChangeRequestRecord record) {
    return new ChangeRequestResponse(
        record.id(),
        record.bizNo(),
        record.targetType(),
        record.targetId(),
        record.targetBizNo(),
        record.changeType(),
        record.reason(),
        record.status(),
        record.requestedBy(),
        record.requestedAt(),
        record.effectiveAt(),
        record.attachmentFileId(),
        record.items().stream().map(ChangeRequestItemResponse::from).toList()
    );
  }
}
