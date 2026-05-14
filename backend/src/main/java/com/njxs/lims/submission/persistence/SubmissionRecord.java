package com.njxs.lims.submission.persistence;

import java.util.UUID;

public record SubmissionRecord(
    UUID id,
    String bizNo,
    UUID customerOrgId,
    UUID customerContactId,
    UUID manufacturerId,
    UUID applicantUserId,
    UUID assignedSalesUserId,
    String formCode,
    int schemaVersion,
    String dataJson,
    String status,
    String officialReportEmail,
    String shippingInfo,
    boolean requiresGlp,
    boolean requiresHardCopyOfElectronicReport,
    String reportLanguage,
    String protocolLanguage,
    String submittedQuantityText,
    String sampleDisposition
) {
}
