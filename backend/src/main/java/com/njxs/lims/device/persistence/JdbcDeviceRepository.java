package com.njxs.lims.device.persistence;

import com.njxs.lims.device.domain.DeviceFileRecord;
import com.njxs.lims.device.domain.DeviceRecord;
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
public class JdbcDeviceRepository implements DeviceRepository {

  private static final String DEVICE_COLUMNS = """
      id, device_no, name, model, manufacturer, location, owner_user_id, metrology_required, status
      """;
  private static final String FILE_COLUMNS = """
      id, device_id, file_type, file_id, version_no, valid_from, valid_until,
      is_current, uploaded_by, uploaded_at, status
      """;

  private final JdbcTemplate jdbcTemplate;

  public JdbcDeviceRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public List<DeviceRecord> list(String keyword, int limit, int offset) {
    if (keyword == null || keyword.isBlank()) {
      return jdbcTemplate.query(
          "select " + DEVICE_COLUMNS + """
          from device
          where deleted_at is null
          order by device_no
          limit ? offset ?
          """,
          (rs, rowNum) -> device(rs),
          limit,
          offset
      );
    }
    String pattern = "%" + keyword.trim().toLowerCase() + "%";
    return jdbcTemplate.query(
        "select " + DEVICE_COLUMNS + """
        from device
        where deleted_at is null
          and (lower(device_no) like ? or lower(name) like ?)
        order by device_no
        limit ? offset ?
        """,
        (rs, rowNum) -> device(rs),
        pattern,
        pattern,
        limit,
        offset
    );
  }

  @Override
  public int count(String keyword) {
    Integer count;
    if (keyword == null || keyword.isBlank()) {
      count = jdbcTemplate.queryForObject(
          "select count(*) from device where deleted_at is null",
          Integer.class
      );
    } else {
      String pattern = "%" + keyword.trim().toLowerCase() + "%";
      count = jdbcTemplate.queryForObject(
          """
          select count(*)
          from device
          where deleted_at is null
            and (lower(device_no) like ? or lower(name) like ?)
          """,
          Integer.class,
          pattern,
          pattern
      );
    }
    return count == null ? 0 : count;
  }

  @Override
  public boolean existsDevice(UUID id) {
    Integer count = jdbcTemplate.queryForObject(
        "select count(*) from device where id = ? and deleted_at is null",
        Integer.class,
        id
    );
    return count != null && count > 0;
  }

  @Override
  public boolean existsFile(UUID fileId) {
    Integer count = jdbcTemplate.queryForObject(
        "select count(*) from file_metadata where id = ? and deleted_at is null",
        Integer.class,
        fileId
    );
    return count != null && count > 0;
  }

  @Override
  public List<DeviceFileRecord> listFiles(UUID deviceId) {
    return jdbcTemplate.query(
        "select " + FILE_COLUMNS + """
        from device_file
        where device_id = ?
          and deleted_at is null
        order by is_current desc, uploaded_at desc, file_type
        """,
        (rs, rowNum) -> file(rs),
        deviceId
    );
  }

  @Override
  public Optional<DeviceFileRecord> findFileById(UUID id) {
    return jdbcTemplate.query(
        "select " + FILE_COLUMNS + """
        from device_file
        where id = ?
          and deleted_at is null
        """,
        (rs, rowNum) -> file(rs),
        id
    ).stream().findFirst();
  }

  @Override
  public DeviceFileRecord saveFile(DeviceFileRecord record, UUID actorUserId) {
    jdbcTemplate.update(
        """
        insert into device_file (
          id, device_id, file_type, file_id, version_no, valid_from, valid_until,
          is_current, uploaded_by, uploaded_at, status, created_at, created_by,
          updated_at, updated_by, version, source_type, data_quality_score
        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now(), ?, now(), ?, 0, 'NEW', 100)
        """,
        record.id(),
        record.deviceId(),
        record.fileType(),
        record.fileId(),
        record.versionNo(),
        record.validFrom(),
        record.validUntil(),
        record.current(),
        record.uploadedBy(),
        Timestamp.from(record.uploadedAt()),
        record.status(),
        actorUserId,
        actorUserId
    );
    return record;
  }

  @Override
  public Optional<DeviceFileRecord> disableFile(UUID id, UUID actorUserId) {
    return jdbcTemplate.query(
        "update device_file " + """
        set status = 'DISABLED',
            is_current = false,
            updated_at = now(),
            updated_by = ?,
            version = version + 1
        where id = ?
          and status = 'ACTIVE'
          and deleted_at is null
        returning
        """ + FILE_COLUMNS,
        (rs, rowNum) -> file(rs),
        actorUserId,
        id
    ).stream().findFirst();
  }

  @Override
  public boolean deviceUsedByAssignedWorkOrder(UUID deviceId, UUID userId) {
    Integer count = jdbcTemplate.queryForObject(
        """
        select count(*)
        from equipment_usage eu
        join raw_record rr on rr.id = eu.raw_record_id
        join work_order wo on wo.id = rr.work_order_id
        where eu.device_id = ?
          and wo.owner_user_id = ?
          and eu.deleted_at is null
          and rr.deleted_at is null
          and wo.deleted_at is null
        """,
        Integer.class,
        deviceId,
        userId
    );
    return count != null && count > 0;
  }

  @Override
  public void clearCurrent(UUID deviceId, String fileType, UUID actorUserId) {
    jdbcTemplate.update(
        """
        update device_file
        set is_current = false,
            updated_at = now(),
            updated_by = ?,
            version = version + 1
        where device_id = ?
          and file_type = ?
          and is_current = true
          and status = 'ACTIVE'
          and deleted_at is null
        """,
        actorUserId,
        deviceId,
        fileType
    );
  }

  private DeviceRecord device(ResultSet rs) throws SQLException {
    return new DeviceRecord(
        rs.getObject("id", UUID.class),
        rs.getString("device_no"),
        rs.getString("name"),
        rs.getString("model"),
        rs.getString("manufacturer"),
        rs.getString("location"),
        rs.getObject("owner_user_id", UUID.class),
        rs.getBoolean("metrology_required"),
        rs.getString("status")
    );
  }

  private DeviceFileRecord file(ResultSet rs) throws SQLException {
    return new DeviceFileRecord(
        rs.getObject("id", UUID.class),
        rs.getObject("device_id", UUID.class),
        rs.getString("file_type"),
        rs.getObject("file_id", UUID.class),
        rs.getString("version_no"),
        rs.getObject("valid_from", java.time.LocalDate.class),
        rs.getObject("valid_until", java.time.LocalDate.class),
        rs.getBoolean("is_current"),
        rs.getObject("uploaded_by", UUID.class),
        instant(rs.getTimestamp("uploaded_at")),
        rs.getString("status")
    );
  }

  private Instant instant(Timestamp timestamp) {
    return timestamp == null ? null : timestamp.toInstant();
  }
}
