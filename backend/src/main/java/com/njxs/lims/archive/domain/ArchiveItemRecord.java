package com.njxs.lims.archive.domain;

import java.time.Instant;
import java.util.UUID;

public record ArchiveItemRecord(
    UUID id,
    UUID projectId,
    String entityType,
    UUID entityId,
    String archiveType,
    UUID fileId,
    String status,
    Instant archivedAt,
    UUID archivedBy
) {
}
