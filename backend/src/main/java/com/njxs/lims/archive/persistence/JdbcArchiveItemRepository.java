package com.njxs.lims.archive.persistence;

import com.njxs.lims.archive.domain.ArchiveItemRecord;
import com.njxs.lims.permission.InternalDataScope;
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
public class JdbcArchiveItemRepository implements ArchiveItemRepository {

  private static final String SELECT_COLUMNS = """
      id, project_id, entity_type, entity_id, archive_type, file_id, status, archived_at, archived_by
      """;

  private final JdbcTemplate jdbcTemplate;

  public JdbcArchiveItemRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public ArchiveItemRecord save(ArchiveItemRecord record) {
    jdbcTemplate.update(
        """
        insert into archive_item (
          id, project_id, entity_type, entity_id, archive_type, file_id, status,
          archived_at, archived_by, created_at, created_by, updated_at, updated_by,
          version, source_type, data_quality_score
        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        record.id(),
        record.projectId(),
        record.entityType(),
        record.entityId(),
        record.archiveType(),
        record.fileId(),
        record.status(),
        timestamp(record.archivedAt()),
        record.archivedBy(),
        timestamp(record.archivedAt()),
        record.archivedBy(),
        timestamp(record.archivedAt()),
        record.archivedBy(),
        0,
        "NEW",
        100
    );
    return record;
  }

  @Override
  public List<ArchiveItemRecord> list(UUID projectId, int limit, int offset) {
    if (projectId == null) {
      return jdbcTemplate.query(
          "select " + SELECT_COLUMNS + """
          from archive_item
          where deleted_at is null
          order by archived_at desc
          limit ? offset ?
          """,
          (rs, rowNum) -> record(rs),
          limit,
          offset
      );
    }
    return jdbcTemplate.query(
        "select " + SELECT_COLUMNS + """
        from archive_item
        where deleted_at is null and project_id = ?
        order by archived_at desc
        limit ? offset ?
        """,
        (rs, rowNum) -> record(rs),
        projectId,
        limit,
        offset
    );
  }

  @Override
  public int count(UUID projectId) {
    Integer count = projectId == null
        ? jdbcTemplate.queryForObject(
            "select count(*) from archive_item where deleted_at is null",
            Integer.class
        )
        : jdbcTemplate.queryForObject(
            "select count(*) from archive_item where deleted_at is null and project_id = ?",
            Integer.class,
            projectId
        );
    return count == null ? 0 : count;
  }

  @Override
  public List<ArchiveItemRecord> listVisibleTo(
      InternalDataScope scope,
      UUID projectId,
      int limit,
      int offset
  ) {
    List<Object> args = new ArrayList<>();
    String predicate = visibleArchivePredicate(scope, args);
    String projectFilter = "";
    if (projectId != null) {
      projectFilter = " and ai.project_id = ?\n";
      args.add(projectId);
    }
    args.add(limit);
    args.add(offset);
    return jdbcTemplate.query(
        "select ai." + SELECT_COLUMNS.strip().replaceAll("\\s+", " ").replace(", ", ", ai.") + " " + """
        from archive_item ai
        join project p on p.id = ai.project_id and p.deleted_at is null
        left join submission sub on sub.id = p.submission_id and sub.deleted_at is null
        where ai.deleted_at is null
          and (%s)
        """.formatted(predicate) + projectFilter + """
        order by ai.archived_at desc
        limit ? offset ?
        """,
        (rs, rowNum) -> record(rs),
        args.toArray()
    );
  }

  @Override
  public int countVisibleTo(InternalDataScope scope, UUID projectId) {
    List<Object> args = new ArrayList<>();
    String predicate = visibleArchivePredicate(scope, args);
    String projectFilter = "";
    if (projectId != null) {
      projectFilter = " and ai.project_id = ?\n";
      args.add(projectId);
    }
    Integer count = jdbcTemplate.queryForObject(
        """
        select count(*)
        from archive_item ai
        join project p on p.id = ai.project_id and p.deleted_at is null
        left join submission sub on sub.id = p.submission_id and sub.deleted_at is null
        where ai.deleted_at is null
          and (%s)
        """.formatted(predicate) + projectFilter,
        Integer.class,
        args.toArray()
    );
    return count == null ? 0 : count;
  }

  @Override
  public boolean existsProject(UUID projectId) {
    Integer count = jdbcTemplate.queryForObject(
        "select count(*) from project where id = ? and deleted_at is null",
        Integer.class,
        projectId
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

  private String visibleArchivePredicate(InternalDataScope scope, List<Object> args) {
    if (scope.allAccess() || scope.hasRole("ARCHIVE_MANAGER")) {
      return "true";
    }
    List<String> clauses = new ArrayList<>();
    if (scope.hasRole("SALES_REP")) {
      clauses.add("sub.assigned_sales_user_id = ?");
      args.add(scope.userId());
    }
    if (scope.hasRole("TECHNICAL_LEADER")) {
      clauses.add("p.technical_owner_id = ?");
      args.add(scope.userId());
    }
    if (scope.hasRole("BUSINESS_OWNER")) {
      clauses.add("p.business_owner_id = ?");
      args.add(scope.userId());
    }
    if (clauses.isEmpty()) {
      return "false";
    }
    return String.join(" or ", clauses);
  }

  private ArchiveItemRecord record(ResultSet rs) throws SQLException {
    return new ArchiveItemRecord(
        rs.getObject("id", UUID.class),
        rs.getObject("project_id", UUID.class),
        rs.getString("entity_type"),
        rs.getObject("entity_id", UUID.class),
        rs.getString("archive_type"),
        rs.getObject("file_id", UUID.class),
        rs.getString("status"),
        instant(rs.getTimestamp("archived_at")),
        rs.getObject("archived_by", UUID.class)
    );
  }

  private Timestamp timestamp(Instant instant) {
    return instant == null ? null : Timestamp.from(instant);
  }

  private Instant instant(Timestamp timestamp) {
    return timestamp == null ? null : timestamp.toInstant();
  }
}
