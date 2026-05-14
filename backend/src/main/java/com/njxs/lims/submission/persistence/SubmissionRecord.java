package com.njxs.lims.submission.persistence;

import java.util.UUID;

public record SubmissionRecord(
    UUID id,
    String bizNo,
    UUID customerOrgId,
    UUID applicantUserId,
    String status
) {
}
