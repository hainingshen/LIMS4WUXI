package com.njxs.lims.report.domain;

import java.util.UUID;

public record ReportWorkOrderRecord(
    UUID workOrderId,
    UUID rawRecordId,
    String status
) {
}
