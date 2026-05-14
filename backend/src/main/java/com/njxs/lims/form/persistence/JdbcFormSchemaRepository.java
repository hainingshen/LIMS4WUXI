package com.njxs.lims.form.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.njxs.lims.form.domain.FormSchemaRecord;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix = "lims.persistence", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JdbcFormSchemaRepository implements FormSchemaRepository {

  private final JdbcTemplate jdbcTemplate;
  private final ObjectMapper objectMapper;

  public JdbcFormSchemaRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
    this.jdbcTemplate = jdbcTemplate;
    this.objectMapper = objectMapper;
  }

  @Override
  public List<FormSchemaRecord> find(String formCode, String status) {
    StringBuilder sql = new StringBuilder(
        """
        select id, form_code, version_no, schema_json, ui_schema_json, status, published_at, published_by, note
        from form_schema
        where 1 = 1
        """
    );
    List<Object> args = new ArrayList<>();
    if (formCode != null && !formCode.isBlank()) {
      sql.append(" and form_code = ?");
      args.add(formCode);
    }
    if (status != null && !status.isBlank()) {
      sql.append(" and status = ?");
      args.add(status);
    }
    sql.append(" order by form_code, version_no desc");
    return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> record(rs), args.toArray());
  }

  private FormSchemaRecord record(ResultSet rs) throws SQLException {
    return new FormSchemaRecord(
        rs.getObject("id", UUID.class),
        rs.getString("form_code"),
        rs.getInt("version_no"),
        json(rs.getString("schema_json")),
        json(rs.getString("ui_schema_json")),
        rs.getString("status"),
        instant(rs.getTimestamp("published_at")),
        rs.getObject("published_by", UUID.class),
        rs.getString("note")
    );
  }

  private JsonNode json(String value) {
    if (value == null) {
      return null;
    }
    try {
      return objectMapper.readTree(value);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Invalid form schema JSON", exception);
    }
  }

  private Instant instant(Timestamp timestamp) {
    if (timestamp == null) {
      return null;
    }
    return timestamp.toInstant();
  }
}
