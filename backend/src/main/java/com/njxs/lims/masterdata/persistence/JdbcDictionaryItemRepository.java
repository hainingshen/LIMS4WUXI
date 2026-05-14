package com.njxs.lims.masterdata.persistence;

import com.njxs.lims.masterdata.domain.DictionaryItemRecord;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix = "lims.persistence", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JdbcDictionaryItemRepository implements DictionaryItemRepository {

  private final JdbcTemplate jdbcTemplate;

  public JdbcDictionaryItemRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public List<DictionaryItemRecord> find(String dictCode) {
    StringBuilder sql = new StringBuilder(
        """
        select dict_code, item_code, label_zh, label_en, sort_order, enabled
        from dictionary_item
        where enabled = true
        """
    );
    List<Object> args = new ArrayList<>();
    if (dictCode != null && !dictCode.isBlank()) {
      sql.append(" and dict_code = ?");
      args.add(dictCode);
    }
    sql.append(" order by dict_code, sort_order, item_code");
    return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> record(rs), args.toArray());
  }

  private DictionaryItemRecord record(ResultSet rs) throws SQLException {
    return new DictionaryItemRecord(
        rs.getString("dict_code"),
        rs.getString("item_code"),
        rs.getString("label_zh"),
        rs.getString("label_en"),
        rs.getInt("sort_order"),
        rs.getBoolean("enabled")
    );
  }
}
