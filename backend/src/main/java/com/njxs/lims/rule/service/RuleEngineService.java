package com.njxs.lims.rule.service;

import java.util.Map;

public interface RuleEngineService {

  boolean evaluate(String ruleCode, Map<String, Object> context);

  void invalidate(String ruleCode);

  void invalidateAll();
}
