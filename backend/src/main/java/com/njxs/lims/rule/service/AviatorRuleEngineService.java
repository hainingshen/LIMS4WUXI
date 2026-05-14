package com.njxs.lims.rule.service;

import com.googlecode.aviator.AviatorEvaluator;
import com.googlecode.aviator.Expression;
import com.njxs.lims.common.exception.BusinessException;
import com.njxs.lims.rule.domain.BusinessRule;
import com.njxs.lims.rule.domain.RuleCacheInvalidatedEvent;
import com.njxs.lims.rule.persistence.BusinessRuleRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "lims.persistence", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AviatorRuleEngineService implements RuleEngineService {

  private final BusinessRuleRepository businessRuleRepository;
  private final Duration cacheTtl;
  private final Clock clock;
  private final Map<String, CachedRule> cache = new ConcurrentHashMap<>();

  @Autowired
  public AviatorRuleEngineService(
      BusinessRuleRepository businessRuleRepository,
      @Value("${lims.rules.cache-ttl:60s}") Duration cacheTtl
  ) {
    this(businessRuleRepository, cacheTtl, Clock.systemUTC());
  }

  AviatorRuleEngineService(BusinessRuleRepository businessRuleRepository, Duration cacheTtl, Clock clock) {
    this.businessRuleRepository = businessRuleRepository;
    this.cacheTtl = cacheTtl;
    this.clock = clock;
  }

  @Override
  public boolean evaluate(String ruleCode, Map<String, Object> context) {
    String normalizedRuleCode = requireRuleCode(ruleCode);
    CachedRule cachedRule = cache.compute(
        normalizedRuleCode,
        (key, existing) -> existing != null && !expired(existing) ? existing : loadRule(key)
    );
    return booleanResult(cachedRule.rule(), cachedRule.expression().execute(new HashMap<>(context)));
  }

  @Override
  public void invalidate(String ruleCode) {
    if (ruleCode == null || ruleCode.isBlank()) {
      invalidateAll();
      return;
    }
    cache.remove(ruleCode);
  }

  @Override
  public void invalidateAll() {
    cache.clear();
  }

  @EventListener
  public void onRuleCacheInvalidated(RuleCacheInvalidatedEvent event) {
    invalidate(event.ruleCode());
  }

  private CachedRule loadRule(String ruleCode) {
    BusinessRule rule = businessRuleRepository.findEnabledByRuleCode(ruleCode)
        .orElseThrow(() -> new BusinessException(
            "BUSINESS_RULE_NOT_FOUND",
            "业务规则不存在或未启用",
            HttpStatus.NOT_FOUND,
            Map.of("ruleCode", ruleCode)
        ));
    return new CachedRule(rule, AviatorEvaluator.compile(rule.conditionExpr(), true), clock.instant());
  }

  private boolean expired(CachedRule cachedRule) {
    return !clock.instant().isBefore(cachedRule.loadedAt().plus(cacheTtl));
  }

  private boolean booleanResult(BusinessRule rule, Object result) {
    if (result instanceof Boolean booleanValue) {
      return booleanValue;
    }
    throw new BusinessException(
        "BUSINESS_RULE_EVALUATION_FAILED",
        "业务规则求值失败",
        HttpStatus.BAD_REQUEST,
        Map.of("ruleCode", rule.ruleCode(), "resultType", result == null ? "null" : result.getClass().getName())
    );
  }

  private String requireRuleCode(String ruleCode) {
    if (ruleCode == null || ruleCode.isBlank()) {
      throw new BusinessException(
          "VALIDATION_FAILED",
          "请求参数校验失败",
          HttpStatus.BAD_REQUEST,
          Map.of("ruleCode", "required")
      );
    }
    return ruleCode;
  }

  private record CachedRule(BusinessRule rule, Expression expression, Instant loadedAt) {
  }
}
