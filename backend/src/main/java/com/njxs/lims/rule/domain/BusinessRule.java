package com.njxs.lims.rule.domain;

import java.util.UUID;

public record BusinessRule(
    UUID id,
    String ruleCode,
    String nameZh,
    String triggerType,
    String conditionExpr,
    String actionCode,
    String actionPayloadJson,
    int priority,
    boolean enabled,
    String note
) {
}
