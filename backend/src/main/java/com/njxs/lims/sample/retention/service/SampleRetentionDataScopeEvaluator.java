package com.njxs.lims.sample.retention.service;

import com.njxs.lims.auth.domain.AuthPrincipal;
import com.njxs.lims.permission.DataScopeEvaluator;
import com.njxs.lims.sample.retention.persistence.SampleRetentionRepository;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "lims.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SampleRetentionDataScopeEvaluator implements DataScopeEvaluator {

  private static final String ACTION_DISPOSE = "SAMPLE_RETENTION_DISPOSE";
  private static final String ACTION_EXTEND = "SAMPLE_RETENTION_EXTEND";

  private final SampleRetentionRepository retentionRepository;

  public SampleRetentionDataScopeEvaluator(SampleRetentionRepository retentionRepository) {
    this.retentionRepository = retentionRepository;
  }

  @Override
  public boolean supports(String actionCode, String scope) {
    return (ACTION_DISPOSE.equals(actionCode)
        && ("PAYMENT_UNLOCKED".equals(scope) || "EXCEPTION_REVIEW".equals(scope)))
        || (ACTION_EXTEND.equals(actionCode) && "SAMPLE_SCOPE".equals(scope));
  }

  @Override
  public boolean isAllowed(AuthPrincipal principal, String actionCode, String scope, Object[] args) {
    Optional<UUID> retentionId = firstUuid(args);
    if (retentionId.isEmpty()) {
      return false;
    }
    if (ACTION_DISPOSE.equals(actionCode) && "PAYMENT_UNLOCKED".equals(scope)) {
      return retentionRepository.paymentUnlocked(retentionId.get());
    }
    return retentionRepository.findById(retentionId.get()).isPresent();
  }

  private Optional<UUID> firstUuid(Object[] args) {
    return Arrays.stream(args)
        .filter(UUID.class::isInstance)
        .map(UUID.class::cast)
        .findFirst();
  }
}
