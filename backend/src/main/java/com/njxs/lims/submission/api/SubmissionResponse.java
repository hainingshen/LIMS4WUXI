package com.njxs.lims.submission.api;

import com.njxs.lims.submission.persistence.SubmissionRecord;
import java.util.UUID;

public record SubmissionResponse(
    UUID id,
    String bizNo,
    UUID customerOrgId,
    UUID customerContactId,
    UUID manufacturerId,
    UUID applicantUserId,
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

  public static SubmissionResponse from(SubmissionRecord record) {
    return new SubmissionResponse(
        record.id(),
        record.bizNo(),
        record.customerOrgId(),
        record.customerContactId(),
        record.manufacturerId(),
        record.applicantUserId(),
        record.status(),
        record.officialReportEmail(),
        record.shippingInfo(),
        record.requiresGlp(),
        record.requiresHardCopyOfElectronicReport(),
        record.reportLanguage(),
        record.protocolLanguage(),
        record.submittedQuantityText(),
        record.sampleDisposition()
    );
  }
}
