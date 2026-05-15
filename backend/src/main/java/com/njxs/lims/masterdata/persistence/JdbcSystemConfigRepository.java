package com.njxs.lims.masterdata.persistence;

import com.njxs.lims.masterdata.domain.SystemConfigRecord;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix = "lims.persistence", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JdbcSystemConfigRepository implements SystemConfigRepository {

  private static final String SELECT_COLUMNS = """
      id, config_key, config_value, value_type, category, editable_by_role, description_zh
      """;

  private final JdbcTemplate jdbcTemplate;

  public JdbcSystemConfigRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public List<SystemConfigRecord> list() {
    return jdbcTemplate.query(
        "select " + SELECT_COLUMNS + """
        from system_config
        order by category, config_key
        """,
        (rs, rowNum) -> record(rs)
    );
  }

  @Override
  public SystemConfigRecord upsert(SystemConfigRecord record) {
    return jdbcTemplate.queryForObject(
        "insert into system_config " + """
        (
          id, config_key, config_value, value_type, category, editable_by_role, description_zh,
          created_at, updated_at
        ) values (?, ?, ?, ?, ?, ?, ?, now(), now())
        on conflict (config_key) do update set
          config_value = excluded.config_value,
          value_type = excluded.value_type,
          category = excluded.category,
          editable_by_role = excluded.editable_by_role,
          description_zh = excluded.description_zh,
          updated_at = now()
        returning
        """ + SELECT_COLUMNS,
        (rs, rowNum) -> record(rs),
        record.id(),
        record.configKey(),
        record.configValue(),
        record.valueType(),
        record.category(),
        record.editableByRole(),
        record.descriptionZh()
    );
  }

  private SystemConfigRecord record(ResultSet rs) throws SQLException {
    return new SystemConfigRecord(
        rs.getObject("id", UUID.class),
        rs.getString("config_key"),
        rs.getString("config_value"),
        rs.getString("value_type"),
        rs.getString("category"),
        rs.getString("editable_by_role"),
        rs.getString("description_zh")
    );
  }
}
