package com.njxs.lims.rule.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.njxs.lims.rule.domain.BusinessRule;
import com.njxs.lims.rule.persistence.BusinessRuleRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class AviatorRuleEngineServiceTests {

  @Test
  void expiresCachedRuleWithinConfiguredTtl() {
    AtomicReference<String> expression = new AtomicReference<>("requiresGlp == true");
    MutableClock clock = new MutableClock(Instant.parse("2026-05-14T15:10:00Z"));
    BusinessRuleRepository repository = new BusinessRuleRepository() {
      @Override
      public Optional<BusinessRule> findEnabledByRuleCode(String ruleCode) {
        return Optional.of(rule(ruleCode, expression.get()));
      }

      @Override
      public List<BusinessRule> list() {
        return List.of();
      }

      @Override
      public BusinessRule upsert(BusinessRule rule) {
        return rule;
      }
    };
    RuleEngineService ruleEngineService = new AviatorRuleEngineService(repository, Duration.ofMinutes(1), clock);

    assertThat(ruleEngineService.evaluate("PLAN_REQUIRES_QA", Map.of("requiresGlp", true))).isTrue();

    expression.set("false");
    assertThat(ruleEngineService.evaluate("PLAN_REQUIRES_QA", Map.of("requiresGlp", true))).isTrue();

    clock.advance(Duration.ofMinutes(1));
    assertThat(ruleEngineService.evaluate("PLAN_REQUIRES_QA", Map.of("requiresGlp", true))).isFalse();
  }

  private BusinessRule rule(String ruleCode, String conditionExpr) {
    return new BusinessRule(
        UUID.randomUUID(),
        ruleCode,
        "测试规则",
        "TEST",
        conditionExpr,
        "TEST_ACTION",
        "{}",
        100,
        true,
        null
    );
  }

  private static final class MutableClock extends Clock {

    private Instant instant;

    private MutableClock(Instant instant) {
      this.instant = instant;
    }

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return instant;
    }

    private void advance(Duration duration) {
      instant = instant.plus(duration);
    }
  }
}
