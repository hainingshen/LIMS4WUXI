package com.njxs.lims.submission.service;

import com.njxs.lims.auth.domain.AuthPrincipal;
import com.njxs.lims.permission.DataScopeEvaluator;
import com.njxs.lims.submission.persistence.SubmissionRepository;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "lims.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SubmissionDataScopeEvaluator implements DataScopeEvaluator {

  private static final String ACTION_SUBMISSION_CREATE = "SUBMISSION_CREATE";

  private final SubmissionRepository submissionRepository;

  public SubmissionDataScopeEvaluator(SubmissionRepository submissionRepository) {
    this.submissionRepository = submissionRepository;
  }

  @Override
  public boolean supports(String actionCode, String scope) {
    return ACTION_SUBMISSION_CREATE.equals(actionCode) && ("OWN".equals(scope) || "ORG".equals(scope));
  }

  @Override
  public boolean isAllowed(AuthPrincipal principal, String actionCode, String scope, Object[] args) {
    Optional<UUID> resourceId = firstUuid(args);
    if (resourceId.isEmpty()) {
      return true;
    }
    if ("OWN".equals(scope)) {
      return submissionRepository.existsByApplicant(resourceId.get(), principal.userId());
    }
    return principal.orgId() != null && submissionRepository.existsByCustomerOrg(resourceId.get(), principal.orgId());
  }

  private Optional<UUID> firstUuid(Object[] args) {
    return Arrays.stream(args)
        .filter(UUID.class::isInstance)
        .map(UUID.class::cast)
        .findFirst();
  }
}
