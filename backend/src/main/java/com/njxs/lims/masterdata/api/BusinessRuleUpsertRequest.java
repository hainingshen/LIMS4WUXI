package com.njxs.lims.masterdata.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record BusinessRuleUpsertRequest(
    @NotBlank String ruleCode,
    @NotBlank String nameZh,
    @NotBlank String triggerType,
    @NotBlank String conditionExpr,
    @NotBlank String actionCode,
    Map<String, Object> actionPayload,
    int priority,
    @NotNull Boolean enabled,
    String note
) {
}
