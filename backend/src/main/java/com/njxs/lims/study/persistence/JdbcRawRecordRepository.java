package com.njxs.lims.study.persistence;

import com.njxs.lims.study.domain.RawRecordRecord;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix = "lims.persistence", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JdbcRawRecordRepository implements RawRecordRepository {

  private static final String SELECT_COLUMNS = """
      id, biz_no, work_order_id, project_id, sample_id, template_code, test_item, test_basis,
      test_time, test_location, temperature_celsius, humidity_percent, method, record_data,
      attachment_file_ids, status, tester_id, checker_id, submitted_at, reviewed_at
      """;

  private final JdbcTemplate jdbcTemplate;

  public JdbcRawRecordRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public String nextBizNo(int year) {
    String prefix = "RR-" + year + "-";
    Integer count = jdbcTemplate.queryForObject(
        "select count(*) from raw_record where biz_no like ?",
        Integer.class,
        prefix + "%"
    );
    return prefix + "%05d".formatted((count == null ? 0 : count) + 1);
  }

  @Override
  public RawRecordRecord save(RawRecordRecord record) {
    jdbcTemplate.update(
        """
        insert into raw_record (
          id, biz_no, work_order_id, project_id, sample_id, template_code, test_item, test_basis,
          test_time, test_location, temperature_celsius, humidity_percent, method, record_data,
          attachment_file_ids, status, tester_id, checker_id, submitted_at, reviewed_at,
          created_at, created_by, updated_at, updated_by, version, source_type, data_quality_score
        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?::jsonb, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        record.id(),
        record.bizNo(),
        record.workOrderId(),
        record.projectId(),
        record.sampleId(),
        record.templateCode(),
        record.testItem(),
        record.testBasis(),
        record.testTime(),
        record.testLocation(),
        record.temperatureCelsius(),
        record.humidityPercent(),
        record.method(),
        record.recordData(),
        record.attachmentFileIds(),
        record.status(),
        record.testerId(),
        record.checkerId(),
        timestamp(record.submittedAt()),
        timestamp(record.reviewedAt()),
        timestamp(record.submittedAt()),
        record.testerId(),
        timestamp(record.submittedAt()),
        record.testerId(),
        0,
        "NEW",
        100
    );
    return record;
  }

  @Override
  public Optional<RawRecordRecord> findById(UUID id) {
    return jdbcTemplate.query(
        "select " + SELECT_COLUMNS + """
        from raw_record
        where id = ? and deleted_at is null
        """,
        (rs, rowNum) -> record(rs),
        id
    ).stream().findFirst();
  }

  @Override
  public List<RawRecordRecord> list(UUID workOrderId, int limit, int offset) {
    if (workOrderId == null) {
      return jdbcTemplate.query(
          "select " + SELECT_COLUMNS + """
          from raw_record
          where deleted_at is null
          order by submitted_at desc, biz_no desc
          limit ? offset ?
          """,
          (rs, rowNum) -> record(rs),
          limit,
          offset
      );
    }
    return jdbcTemplate.query(
        "select " + SELECT_COLUMNS + """
        from raw_record
        where work_order_id = ? and deleted_at is null
        order by submitted_at desc, biz_no desc
        limit ? offset ?
        """,
        (rs, rowNum) -> record(rs),
        workOrderId,
        limit,
        offset
    );
  }

  @Override
  public int count(UUID workOrderId) {
    Integer count;
    if (workOrderId == null) {
      count = jdbcTemplate.queryForObject(
          "select count(*) from raw_record where deleted_at is null",
          Integer.class
      );
    } else {
      count = jdbcTemplate.queryForObject(
          """
          select count(*)
          from raw_record
          where work_order_id = ? and deleted_at is null
          """,
          Integer.class,
          workOrderId
      );
    }
    return count == null ? 0 : count;
  }

  @Override
  public RawRecordRecord updateReview(
      UUID id,
      String status,
      UUID checkerId,
      Instant reviewedAt,
      UUID actorUserId
  ) {
    return jdbcTemplate.queryForObject(
        "update raw_record " + """
        set status = ?,
            checker_id = ?,
            reviewed_at = ?,
            updated_at = now(),
            updated_by = ?,
            version = version + 1
        where id = ? and deleted_at is null
        returning
        """ + SELECT_COLUMNS,
        (rs, rowNum) -> record(rs),
        status,
        checkerId,
        timestamp(reviewedAt),
        actorUserId,
        id
    );
  }

  private RawRecordRecord record(ResultSet rs) throws SQLException {
    return new RawRecordRecord(
        rs.getObject("id", UUID.class),
        rs.getString("biz_no"),
        rs.getObject("work_order_id", UUID.class),
        rs.getObject("project_id", UUID.class),
        rs.getObject("sample_id", UUID.class),
        rs.getString("template_code"),
        rs.getString("test_item"),
        rs.getString("test_basis"),
        rs.getString("test_time"),
        rs.getString("test_location"),
        rs.getString("temperature_celsius"),
        rs.getString("humidity_percent"),
        rs.getString("method"),
        rs.getString("record_data"),
        rs.getString("attachment_file_ids"),
        rs.getString("status"),
        rs.getObject("tester_id", UUID.class),
        rs.getObject("checker_id", UUID.class),
        instant(rs.getTimestamp("submitted_at")),
        instant(rs.getTimestamp("reviewed_at"))
    );
  }

  private Timestamp timestamp(Instant instant) {
    return instant == null ? null : Timestamp.from(instant);
  }

  private Instant instant(Timestamp timestamp) {
    return timestamp == null ? null : timestamp.toInstant();
  }
}
