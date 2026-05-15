package com.njxs.lims.device.persistence;

import com.njxs.lims.device.domain.DeviceBookingRecord;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix = "lims.persistence", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JdbcDeviceBookingRepository implements DeviceBookingRepository {

  private static final String BOOKING_COLUMNS = """
      id, biz_no, device_id, booker_id, booker_type, work_order_id, purpose,
      start_at, end_at, status, override_reason
      """;

  private final JdbcTemplate jdbcTemplate;

  public JdbcDeviceBookingRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public String nextBizNo(LocalDate date) {
    String prefix = "BK-" + date.format(DateTimeFormatter.BASIC_ISO_DATE) + "-";
    Integer count = jdbcTemplate.queryForObject(
        "select count(*) from device_booking where biz_no like ?",
        Integer.class,
        prefix + "%"
    );
    return prefix + "%04d".formatted((count == null ? 0 : count) + 1);
  }

  @Override
  public List<DeviceBookingRecord> list(UUID deviceId, int limit, int offset) {
    if (deviceId == null) {
      return jdbcTemplate.query(
          "select " + BOOKING_COLUMNS + """
          from device_booking
          where deleted_at is null
          order by start_at desc, biz_no desc
          limit ? offset ?
          """,
          (rs, rowNum) -> booking(rs),
          limit,
          offset
      );
    }
    return jdbcTemplate.query(
        "select " + BOOKING_COLUMNS + """
        from device_booking
        where deleted_at is null
          and device_id = ?
        order by start_at desc, biz_no desc
        limit ? offset ?
        """,
        (rs, rowNum) -> booking(rs),
        deviceId,
        limit,
        offset
    );
  }

  @Override
  public int count(UUID deviceId) {
    Integer count = deviceId == null
        ? jdbcTemplate.queryForObject(
            "select count(*) from device_booking where deleted_at is null",
            Integer.class
        )
        : jdbcTemplate.queryForObject(
            "select count(*) from device_booking where deleted_at is null and device_id = ?",
            Integer.class,
            deviceId
        );
    return count == null ? 0 : count;
  }

  @Override
  public Optional<DeviceBookingRecord> findById(UUID id) {
    return jdbcTemplate.query(
        "select " + BOOKING_COLUMNS + """
        from device_booking
        where id = ?
          and deleted_at is null
        """,
        (rs, rowNum) -> booking(rs),
        id
    ).stream().findFirst();
  }

  @Override
  public DeviceBookingRecord save(DeviceBookingRecord record, UUID actorUserId) {
    return jdbcTemplate.queryForObject(
        "insert into device_booking " + """
        (
          id, biz_no, device_id, booker_id, booker_type, work_order_id, purpose,
          start_at, end_at, status, override_reason, created_at, created_by,
          updated_at, updated_by, version, source_type, data_quality_score
        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now(), ?, now(), ?, 0, 'NEW', 100)
        returning
        """ + BOOKING_COLUMNS,
        (rs, rowNum) -> booking(rs),
        record.id(),
        record.bizNo(),
        record.deviceId(),
        record.bookerId(),
        record.bookerType(),
        record.workOrderId(),
        record.purpose(),
        timestamp(record.startAt()),
        timestamp(record.endAt()),
        record.status(),
        record.overrideReason(),
        actorUserId,
        actorUserId
    );
  }

  @Override
  public Optional<DeviceBookingRecord> markNeedsReschedule(UUID id, String reason, UUID actorUserId) {
    return jdbcTemplate.query(
        "update device_booking " + """
        set status = 'NEEDS_RESCHEDULE',
            override_reason = ?,
            overridden_by = ?,
            overridden_at = now(),
            updated_at = now(),
            updated_by = ?,
            version = version + 1
        where id = ?
          and deleted_at is null
        returning
        """ + BOOKING_COLUMNS,
        (rs, rowNum) -> booking(rs),
        reason,
        actorUserId,
        actorUserId,
        id
    ).stream().findFirst();
  }

  @Override
  public boolean existsActiveDevice(UUID deviceId) {
    Integer count = jdbcTemplate.queryForObject(
        """
        select count(*)
        from device
        where id = ?
          and status = 'ACTIVE'
          and deleted_at is null
        """,
        Integer.class,
        deviceId
    );
    return count != null && count > 0;
  }

  @Override
  public boolean existsWorkOrder(UUID workOrderId) {
    Integer count = jdbcTemplate.queryForObject(
        "select count(*) from work_order where id = ? and deleted_at is null",
        Integer.class,
        workOrderId
    );
    return count != null && count > 0;
  }

  @Override
  public boolean hasBookedOverlap(UUID deviceId, Instant startAt, Instant endAt, UUID excludedBookingId) {
    Integer count;
    if (excludedBookingId == null) {
      count = jdbcTemplate.queryForObject(
          """
          select count(*)
          from device_booking
          where device_id = ?
            and status = 'BOOKED'
            and deleted_at is null
            and ? < end_at
            and ? > start_at
          """,
          Integer.class,
          deviceId,
          timestamp(startAt),
          timestamp(endAt)
      );
    } else {
      count = jdbcTemplate.queryForObject(
          """
          select count(*)
          from device_booking
          where device_id = ?
            and id <> ?
            and status = 'BOOKED'
            and deleted_at is null
            and ? < end_at
            and ? > start_at
          """,
          Integer.class,
          deviceId,
          excludedBookingId,
          timestamp(startAt),
          timestamp(endAt)
      );
    }
    return count != null && count > 0;
  }

  private DeviceBookingRecord booking(ResultSet rs) throws SQLException {
    return new DeviceBookingRecord(
        rs.getObject("id", UUID.class),
        rs.getString("biz_no"),
        rs.getObject("device_id", UUID.class),
        rs.getObject("booker_id", UUID.class),
        rs.getString("booker_type"),
        rs.getObject("work_order_id", UUID.class),
        rs.getString("purpose"),
        instant(rs.getTimestamp("start_at")),
        instant(rs.getTimestamp("end_at")),
        rs.getString("status"),
        rs.getString("override_reason")
    );
  }

  private Timestamp timestamp(Instant instant) {
    return instant == null ? null : Timestamp.from(instant);
  }

  private Instant instant(Timestamp timestamp) {
    return timestamp == null ? null : timestamp.toInstant();
  }
}
