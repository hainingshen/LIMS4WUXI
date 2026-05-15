package com.njxs.lims.masterdata.api;

import com.njxs.lims.rule.domain.BusinessRule;
import java.util.Map;
import java.util.UUID;

public record BusinessRuleResponse(
    UUID id,
    String ruleCode,
    String nameZh,
    String triggerType,
    String conditionExpr,
    String actionCode,
    Map<String, Object> actionPayload,
    int priority,
    boolean enabled,
    String note
) {
  public static BusinessRuleResponse from(BusinessRule rule, Map<String, Object> actionPayload) {
    return new BusinessRuleResponse(
        rule.id(),
        rule.ruleCode(),
        rule.nameZh(),
        rule.triggerType(),
        rule.conditionExpr(),
        rule.actionCode(),
        actionPayload,
        rule.priority(),
        rule.enabled(),
        rule.note()
    );
  }
}
