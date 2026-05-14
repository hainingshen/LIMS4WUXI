package com.njxs.lims.study.service;

import com.njxs.lims.auth.domain.AuthPrincipal;
import com.njxs.lims.permission.DataScopeEvaluator;
import com.njxs.lims.study.persistence.RawRecordRepository;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "lims.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RawRecordDataScopeEvaluator implements DataScopeEvaluator {

  private final RawRecordRepository rawRecordRepository;

  public RawRecordDataScopeEvaluator(RawRecordRepository rawRecordRepository) {
    this.rawRecordRepository = rawRecordRepository;
  }

  @Override
  public boolean supports(String actionCode, String scope) {
    return "RAW_RECORD_REVIEW".equals(actionCode) && "TASK".equals(scope);
  }

  @Override
  public boolean isAllowed(AuthPrincipal principal, String actionCode, String scope, Object[] args) {
    return firstUuid(args)
        .flatMap(rawRecordRepository::findById)
        .isPresent();
  }

  private Optional<UUID> firstUuid(Object[] args) {
    return Arrays.stream(args)
        .filter(UUID.class::isInstance)
        .map(UUID.class::cast)
        .findFirst();
  }
}
