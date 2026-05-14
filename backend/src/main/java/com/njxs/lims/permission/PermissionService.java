package com.njxs.lims.permission;

import com.njxs.lims.auth.domain.AuthPrincipal;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "lims.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PermissionService {

  private final PermissionRepository permissionRepository;
  private final List<DataScopeEvaluator> dataScopeEvaluators;

  public PermissionService(
      PermissionRepository permissionRepository,
      List<DataScopeEvaluator> dataScopeEvaluators
  ) {
    this.permissionRepository = permissionRepository;
    this.dataScopeEvaluators = List.copyOf(dataScopeEvaluators);
  }

  public void assertAllowed(AuthPrincipal principal, String actionCode, Object[] args) {
    List<PermissionRule> rules = permissionRepository.findRules(actionCode, principal.roles());
    if (rules.stream().anyMatch(PermissionRule::isDeny)) {
      throw new PermissionDeniedException(actionCode);
    }
    boolean allowed = rules.stream()
        .filter(PermissionRule::grantsAccess)
        .anyMatch(rule -> isInScope(principal, rule, args));
    if (!allowed) {
      throw new PermissionDeniedException(actionCode);
    }
  }

  private boolean isInScope(AuthPrincipal principal, PermissionRule rule, Object[] args) {
    return dataScopeEvaluators.stream()
        .filter(evaluator -> evaluator.supports(rule.actionCode(), rule.scope()))
        .anyMatch(evaluator -> evaluator.isAllowed(principal, rule.actionCode(), rule.scope(), args));
  }
}
