package com.njxs.lims.report.snapshot.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ReportSnapshotSource(
    UUID reportId,
    String reportBizNo,
    String reportStatus,
    String watermarkType,
    String paymentGateStatus,
    String customerOrgNameZh,
    String customerOrgAddressZh,
    String manufacturerNameZh,
    String manufacturerAddressZh,
    UUID workOrderId,
    UUID sampleId,
    String sampleBizNo,
    String sampleNameZh,
    String lotBatch,
    String modelRef,
    String sizeSpec,
    BigDecimal quantity,
    String quantityUnit,
    String physicalState,
    Instant receivedAt,
    LocalDate receivedDate,
    UUID rawRecordId,
    String rawRecordTestItem,
    String rawRecordTestBasis,
    String rawRecordTestTime,
    String rawRecordData
) {
}
