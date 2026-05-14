package com.njxs.lims.submission.persistence;

import java.util.Optional;
import java.util.UUID;

public interface SubmissionRepository {

  Optional<SubmissionRecord> findById(UUID id);

  boolean existsByApplicant(UUID id, UUID applicantUserId);

  boolean existsByCustomerOrg(UUID id, UUID customerOrgId);

  boolean existsByAssignedSales(UUID id, UUID salesUserId);

  SubmissionRecord markSubmitted(UUID id, UUID actorUserId);

  SubmissionRecord markAccepted(UUID id, UUID actorUserId);
}
