package com.njxs.lims.inventory.persistence;

import com.njxs.lims.inventory.domain.InventoryBatchRecord;
import com.njxs.lims.inventory.domain.InventoryTransactionRecord;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix = "lims.persistence", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JdbcInventoryRepository implements InventoryRepository {

  private static final String BATCH_COLUMNS = """
      id, batch_no, name, cas_no, specification, manufacturer, received_at, expiry_date,
      quantity, unit, storage_condition, storage_location, opened_at, status
      """;
  private static final String TXN_COLUMNS = """
      id, inventory_batch_id, txn_type, quantity, operated_by, operated_at, reason
      """;

  private final JdbcTemplate jdbcTemplate;

  public JdbcInventoryRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public List<InventoryBatchRecord> listBatches(String keyword, int limit, int offset) {
    if (keyword == null || keyword.isBlank()) {
      return jdbcTemplate.query(
          "select " + BATCH_COLUMNS + """
          from inventory_batch
          where deleted_at is null
          order by expiry_date nulls last, batch_no
          limit ? offset ?
          """,
          (rs, rowNum) -> batch(rs),
          limit,
          offset
      );
    }
    String pattern = "%" + keyword.trim().toLowerCase() + "%";
    return jdbcTemplate.query(
        "select " + BATCH_COLUMNS + """
        from inventory_batch
        where deleted_at is null
          and (
            lower(batch_no) like ?
            or lower(name) like ?
            or lower(coalesce(cas_no, '')) like ?
          )
        order by expiry_date nulls last, batch_no
        limit ? offset ?
        """,
        (rs, rowNum) -> batch(rs),
        pattern,
        pattern,
        pattern,
        limit,
        offset
    );
  }

  @Override
  public int countBatches(String keyword) {
    Integer count;
    if (keyword == null || keyword.isBlank()) {
      count = jdbcTemplate.queryForObject(
          "select count(*) from inventory_batch where deleted_at is null",
          Integer.class
      );
    } else {
      String pattern = "%" + keyword.trim().toLowerCase() + "%";
      count = jdbcTemplate.queryForObject(
          """
          select count(*)
          from inventory_batch
          where deleted_at is null
            and (
              lower(batch_no) like ?
              or lower(name) like ?
              or lower(coalesce(cas_no, '')) like ?
            )
          """,
          Integer.class,
          pattern,
          pattern,
          pattern
      );
    }
    return count == null ? 0 : count;
  }

  @Override
  public Optional<InventoryBatchRecord> findBatchById(UUID id) {
    return queryBatchById(id, false);
  }

  @Override
  public Optional<InventoryBatchRecord> lockBatchById(UUID id) {
    return queryBatchById(id, true);
  }

  @Override
  public InventoryBatchRecord insertBatch(InventoryBatchRecord record, UUID actorUserId) {
    return jdbcTemplate.queryForObject(
        "insert into inventory_batch " + """
        (
          id, batch_no, name, cas_no, specification, manufacturer, received_at, expiry_date,
          quantity, unit, storage_condition, storage_location, opened_at, status,
          created_at, created_by, updated_at, updated_by, version, source_type, data_quality_score
        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now(), ?, now(), ?, 0, 'NEW', 100)
        returning
        """ + BATCH_COLUMNS,
        (rs, rowNum) -> batch(rs),
        record.id(),
        record.batchNo(),
        record.name(),
        record.casNo(),
        record.specification(),
        record.manufacturer(),
        record.receivedAt(),
        record.expiryDate(),
        record.quantity(),
        record.unit(),
        record.storageCondition(),
        record.storageLocation(),
        record.openedAt(),
        record.status(),
        actorUserId,
        actorUserId
    );
  }

  @Override
  public Optional<InventoryBatchRecord> updateBatch(InventoryBatchRecord record, UUID actorUserId) {
    return jdbcTemplate.query(
        "update inventory_batch " + """
        set batch_no = ?,
            name = ?,
            cas_no = ?,
            specification = ?,
            manufacturer = ?,
            received_at = ?,
            expiry_date = ?,
            quantity = ?,
            unit = ?,
            storage_condition = ?,
            storage_location = ?,
            opened_at = ?,
            status = ?,
            updated_at = now(),
            updated_by = ?,
            version = version + 1
        where id = ?
          and deleted_at is null
        returning
        """ + BATCH_COLUMNS,
        (rs, rowNum) -> batch(rs),
        record.batchNo(),
        record.name(),
        record.casNo(),
        record.specification(),
        record.manufacturer(),
        record.receivedAt(),
        record.expiryDate(),
        record.quantity(),
        record.unit(),
        record.storageCondition(),
        record.storageLocation(),
        record.openedAt(),
        record.status(),
        actorUserId,
        record.id()
    ).stream().findFirst();
  }

  @Override
  public InventoryBatchRecord updateBatchQuantityAndStatus(
      UUID id,
      BigDecimal quantity,
      String status,
      UUID actorUserId
  ) {
    return jdbcTemplate.queryForObject(
        "update inventory_batch " + """
        set quantity = ?,
            status = ?,
            updated_at = now(),
            updated_by = ?,
            version = version + 1
        where id = ?
          and deleted_at is null
        returning
        """ + BATCH_COLUMNS,
        (rs, rowNum) -> batch(rs),
        quantity,
        status,
        actorUserId,
        id
    );
  }

  @Override
  public List<InventoryTransactionRecord> listTransactions(UUID inventoryBatchId, int limit, int offset) {
    if (inventoryBatchId == null) {
      return jdbcTemplate.query(
          "select " + TXN_COLUMNS + """
          from inventory_transaction
          where deleted_at is null
          order by operated_at desc
          limit ? offset ?
          """,
          (rs, rowNum) -> transaction(rs),
          limit,
          offset
      );
    }
    return jdbcTemplate.query(
        "select " + TXN_COLUMNS + """
        from inventory_transaction
        where inventory_batch_id = ?
          and deleted_at is null
        order by operated_at desc
        limit ? offset ?
        """,
        (rs, rowNum) -> transaction(rs),
        inventoryBatchId,
        limit,
        offset
    );
  }

  @Override
  public int countTransactions(UUID inventoryBatchId) {
    Integer count = inventoryBatchId == null
        ? jdbcTemplate.queryForObject(
            "select count(*) from inventory_transaction where deleted_at is null",
            Integer.class
        )
        : jdbcTemplate.queryForObject(
            """
            select count(*)
            from inventory_transaction
            where inventory_batch_id = ?
              and deleted_at is null
            """,
            Integer.class,
            inventoryBatchId
        );
    return count == null ? 0 : count;
  }

  @Override
  public InventoryTransactionRecord saveTransaction(InventoryTransactionRecord record, UUID actorUserId) {
    return jdbcTemplate.queryForObject(
        "insert into inventory_transaction " + """
        (
          id, inventory_batch_id, txn_type, quantity, operated_by, operated_at, reason,
          created_at, created_by, updated_at, updated_by, version, source_type, data_quality_score
        ) values (?, ?, ?, ?, ?, ?, ?, now(), ?, now(), ?, 0, 'NEW', 100)
        returning
        """ + TXN_COLUMNS,
        (rs, rowNum) -> transaction(rs),
        record.id(),
        record.inventoryBatchId(),
        record.txnType(),
        record.quantity(),
        record.operatedBy(),
        timestamp(record.operatedAt()),
        record.reason(),
        actorUserId,
        actorUserId
    );
  }

  private Optional<InventoryBatchRecord> queryBatchById(UUID id, boolean forUpdate) {
    String lockClause = forUpdate ? " for update" : "";
    return jdbcTemplate.query(
        "select " + BATCH_COLUMNS + """
        from inventory_batch
        where id = ?
          and deleted_at is null
        """ + lockClause,
        (rs, rowNum) -> batch(rs),
        id
    ).stream().findFirst();
  }

  private InventoryBatchRecord batch(ResultSet rs) throws SQLException {
    return new InventoryBatchRecord(
        rs.getObject("id", UUID.class),
        rs.getString("batch_no"),
        rs.getString("name"),
        rs.getString("cas_no"),
        rs.getString("specification"),
        rs.getString("manufacturer"),
        rs.getObject("received_at", LocalDate.class),
        rs.getObject("expiry_date", LocalDate.class),
        rs.getBigDecimal("quantity"),
        rs.getString("unit"),
        rs.getString("storage_condition"),
        rs.getString("storage_location"),
        rs.getObject("opened_at", LocalDate.class),
        rs.getString("status")
    );
  }

  private InventoryTransactionRecord transaction(ResultSet rs) throws SQLException {
    return new InventoryTransactionRecord(
        rs.getObject("id", UUID.class),
        rs.getObject("inventory_batch_id", UUID.class),
        rs.getString("txn_type"),
        rs.getBigDecimal("quantity"),
        rs.getObject("operated_by", UUID.class),
        instant(rs.getTimestamp("operated_at")),
        rs.getString("reason")
    );
  }

  private Timestamp timestamp(Instant instant) {
    return instant == null ? null : Timestamp.from(instant);
  }

  private Instant instant(Timestamp timestamp) {
    return timestamp == null ? null : timestamp.toInstant();
  }
}
