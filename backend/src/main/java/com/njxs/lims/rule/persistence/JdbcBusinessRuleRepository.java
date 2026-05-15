package com.njxs.lims.rule.persistence;

import com.njxs.lims.rule.domain.BusinessRule;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix = "lims.persistence", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JdbcBusinessRuleRepository implements BusinessRuleRepository {

  private static final String SELECT_COLUMNS = """
      id, rule_code, name_zh, trigger_type, condition_expr, action_code,
      action_payload::text as action_payload_json, priority, enabled, note
      """;

  private final JdbcTemplate jdbcTemplate;

  public JdbcBusinessRuleRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public Optional<BusinessRule> findEnabledByRuleCode(String ruleCode) {
    return jdbcTemplate.query(
        "select " + SELECT_COLUMNS + """
        from business_rule
        where rule_code = ? and enabled = true
        """,
        (rs, rowNum) -> rule(rs),
        ruleCode
    ).stream().findFirst();
  }

  @Override
  public List<BusinessRule> list() {
    return jdbcTemplate.query(
        "select " + SELECT_COLUMNS + """
        from business_rule
        order by trigger_type, priority, rule_code
        """,
        (rs, rowNum) -> rule(rs)
    );
  }

  @Override
  public BusinessRule upsert(BusinessRule rule) {
    return jdbcTemplate.queryForObject(
        "insert into business_rule " + """
        (
          id, rule_code, name_zh, trigger_type, condition_expr, action_code,
          action_payload, priority, enabled, note, created_at, updated_at
        ) values (?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?, now(), now())
        on conflict (rule_code) do update set
          name_zh = excluded.name_zh,
          trigger_type = excluded.trigger_type,
          condition_expr = excluded.condition_expr,
          action_code = excluded.action_code,
          action_payload = excluded.action_payload,
          priority = excluded.priority,
          enabled = excluded.enabled,
          note = excluded.note,
          updated_at = now()
        returning
        """ + SELECT_COLUMNS,
        (rs, rowNum) -> rule(rs),
        rule.id(),
        rule.ruleCode(),
        rule.nameZh(),
        rule.triggerType(),
        rule.conditionExpr(),
        rule.actionCode(),
        rule.actionPayloadJson(),
        rule.priority(),
        rule.enabled(),
        rule.note()
    );
  }

  private BusinessRule rule(ResultSet rs) throws SQLException {
    return new BusinessRule(
        rs.getObject("id", UUID.class),
        rs.getString("rule_code"),
        rs.getString("name_zh"),
        rs.getString("trigger_type"),
        rs.getString("condition_expr"),
        rs.getString("action_code"),
        rs.getString("action_payload_json"),
        rs.getInt("priority"),
        rs.getBoolean("enabled"),
        rs.getString("note")
    );
  }
}
