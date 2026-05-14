package com.njxs.lims.submission.api;

import com.njxs.lims.submission.persistence.SubmissionRecord;
import java.util.UUID;

public record SubmissionResponse(
    UUID id,
    String bizNo,
    UUID customerOrgId,
    UUID applicantUserId,
    String status
) {

  public static SubmissionResponse from(SubmissionRecord record) {
    return new SubmissionResponse(
        record.id(),
        record.bizNo(),
        record.customerOrgId(),
        record.applicantUserId(),
        record.status()
    );
  }
}
