package com.njxs.lims.report.domain;

import java.util.UUID;

public record ReportVersionRecord(
    UUID id,
    UUID reportId,
    int versionNo,
    String status,
    UUID docxFileId,
    UUID dataSnapshotFileId,
    String renderLog
) {
}
