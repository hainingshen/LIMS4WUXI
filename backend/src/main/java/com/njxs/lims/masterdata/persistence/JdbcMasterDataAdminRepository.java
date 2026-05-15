package com.njxs.lims.masterdata.persistence;

import com.njxs.lims.masterdata.domain.HolidayRecord;
import com.njxs.lims.masterdata.domain.InternalUserRecord;
import com.njxs.lims.masterdata.domain.LegacyStatusMappingRecord;
import com.njxs.lims.masterdata.domain.OrganizationUnitRecord;
import com.njxs.lims.masterdata.domain.RoleRecord;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix = "lims.persistence", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JdbcMasterDataAdminRepository implements MasterDataAdminRepository {

  private static final String INTERNAL_USER_COLUMNS = """
      au.id, au.username, au.display_name, au.email, au.organization_unit_id, au.status,
      coalesce(array_agg(ur.role_code order by ur.role_code) filter (where ur.role_code is not null), '{}') role_codes
      """;

  private final JdbcTemplate jdbcTemplate;

  public JdbcMasterDataAdminRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public List<HolidayRecord> listHolidays() {
    return jdbcTemplate.query(
        """
        select id, holiday_date, type, name
        from holiday
        order by holiday_date
        """,
        (rs, rowNum) -> holiday(rs)
    );
  }

  @Override
  public HolidayRecord upsertHoliday(HolidayRecord record) {
    return jdbcTemplate.queryForObject(
        """
        insert into holiday (id, holiday_date, type, name, created_at, updated_at)
        values (?, ?, ?, ?, now(), now())
        on conflict (holiday_date) do update set
          type = excluded.type,
          name = excluded.name,
          updated_at = now()
        returning id, holiday_date, type, name
        """,
        (rs, rowNum) -> holiday(rs),
        record.id(),
        record.date(),
        record.type(),
        record.name()
    );
  }

  @Override
  public List<OrganizationUnitRecord> listOrganizationUnits() {
    return jdbcTemplate.query(
        """
        select id, parent_id, name, status
        from organization_unit
        order by parent_id nulls first, name
        """,
        (rs, rowNum) -> organizationUnit(rs)
    );
  }

  @Override
  public List<InternalUserRecord> listInternalUsers(int limit, int offset) {
    return jdbcTemplate.query(
        "select " + INTERNAL_USER_COLUMNS + """
        from app_user au
        left join user_role ur on ur.user_id = au.id
        where au.client_type = 'INTERNAL'
          and au.deleted_at is null
        group by au.id
        order by au.username
        limit ? offset ?
        """,
        (rs, rowNum) -> internalUser(rs),
        limit,
        offset
    );
  }

  @Override
  public int countInternalUsers() {
    Integer count = jdbcTemplate.queryForObject(
        "select count(*) from app_user where client_type = 'INTERNAL' and deleted_at is null",
        Integer.class
    );
    return count == null ? 0 : count;
  }

  @Override
  public boolean existsUsername(String username) {
    Integer count = jdbcTemplate.queryForObject(
        "select count(*) from app_user where username = ? and deleted_at is null",
        Integer.class,
        username
    );
    return count != null && count > 0;
  }

  @Override
  public boolean existsOrganizationUnit(UUID id) {
    Integer count = jdbcTemplate.queryForObject(
        "select count(*) from organization_unit where id = ? and status = 'ACTIVE'",
        Integer.class,
        id
    );
    return count != null && count > 0;
  }

  @Override
  public Optional<InternalUserRecord> findInternalUser(UUID id) {
    return jdbcTemplate.query(
        "select " + INTERNAL_USER_COLUMNS + """
        from app_user au
        left join user_role ur on ur.user_id = au.id
        where au.id = ?
          and au.client_type = 'INTERNAL'
          and au.deleted_at is null
        group by au.id
        """,
        (rs, rowNum) -> internalUser(rs),
        id
    ).stream().findFirst();
  }

  @Override
  public InternalUserRecord insertInternalUser(
      UUID id,
      String username,
      String displayName,
      String email,
      UUID organizationUnitId,
      String passwordHash,
      List<String> roleCodes,
      UUID actorUserId
  ) {
    jdbcTemplate.update(
        """
        insert into app_user (
          id, username, display_name, email, organization_unit_id, password_hash, client_type,
          status, created_at, created_by, updated_at, updated_by, version, source_type, data_quality_score
        ) values (?, ?, ?, ?, ?, ?, 'INTERNAL', 'ACTIVE', now(), ?, now(), ?, 0, 'NEW', 100)
        """,
        id,
        username,
        displayName,
        email,
        organizationUnitId,
        passwordHash,
        actorUserId,
        actorUserId
    );
    replaceRoles(id, roleCodes);
    return findInternalUser(id).orElseThrow();
  }

  @Override
  public InternalUserRecord updateInternalUser(
      UUID id,
      String displayName,
      String email,
      UUID organizationUnitId,
      List<String> roleCodes,
      UUID actorUserId
  ) {
    jdbcTemplate.update(
        """
        update app_user
        set display_name = ?,
            email = ?,
            organization_unit_id = ?,
            updated_at = now(),
            updated_by = ?,
            version = version + 1
        where id = ?
          and client_type = 'INTERNAL'
          and deleted_at is null
        """,
        displayName,
        email,
        organizationUnitId,
        actorUserId,
        id
    );
    replaceRoles(id, roleCodes);
    return findInternalUser(id).orElseThrow();
  }

  @Override
  public InternalUserRecord updateInternalUserStatus(UUID id, String status, UUID actorUserId) {
    jdbcTemplate.update(
        """
        update app_user
        set status = ?,
            updated_at = now(),
            updated_by = ?,
            version = version + 1
        where id = ?
          and client_type = 'INTERNAL'
          and deleted_at is null
        """,
        status,
        actorUserId,
        id
    );
    return findInternalUser(id).orElseThrow();
  }

  @Override
  public InternalUserRecord replaceInternalUserRoles(UUID id, List<String> roleCodes, UUID actorUserId) {
    jdbcTemplate.update(
        """
        update app_user
        set updated_at = now(),
            updated_by = ?,
            version = version + 1
        where id = ?
          and client_type = 'INTERNAL'
          and deleted_at is null
        """,
        actorUserId,
        id
    );
    replaceRoles(id, roleCodes);
    return findInternalUser(id).orElseThrow();
  }

  @Override
  public InternalUserRecord resetInternalUserPassword(UUID id, String passwordHash, UUID actorUserId) {
    jdbcTemplate.update(
        """
        update app_user
        set password_hash = ?,
            updated_at = now(),
            updated_by = ?,
            version = version + 1
        where id = ?
          and client_type = 'INTERNAL'
          and deleted_at is null
        """,
        passwordHash,
        actorUserId,
        id
    );
    return findInternalUser(id).orElseThrow();
  }

  @Override
  public List<RoleRecord> listRoles() {
    return jdbcTemplate.query(
        """
        select role_code, label_zh, side, status
        from role_definition
        order by side, role_code
        """,
        (rs, rowNum) -> new RoleRecord(
            rs.getString("role_code"),
            rs.getString("label_zh"),
            rs.getString("side"),
            rs.getString("status")
        )
    );
  }

  @Override
  public Set<String> activeInternalRoleCodes(List<String> roleCodes) {
    if (roleCodes.isEmpty()) {
      return Set.of();
    }
    String placeholders = String.join(", ", roleCodes.stream().map(role -> "?").toList());
    return Set.copyOf(jdbcTemplate.queryForList(
        """
        select role_code
        from role_definition
        where side = 'INTERNAL'
          and status = 'ACTIVE'
          and role_code in (%s)
        """.formatted(placeholders),
        String.class,
        roleCodes.toArray()
    ));
  }

  @Override
  public Optional<RoleRecord> findRole(String roleCode) {
    return jdbcTemplate.query(
        """
        select role_code, label_zh, side, status
        from role_definition
        where role_code = ?
        """,
        (rs, rowNum) -> new RoleRecord(
            rs.getString("role_code"),
            rs.getString("label_zh"),
            rs.getString("side"),
            rs.getString("status")
        ),
        roleCode
    ).stream().findFirst();
  }

  @Override
  public RoleRecord updateRoleStatus(String roleCode, String status) {
    return jdbcTemplate.queryForObject(
        """
        update role_definition
        set status = ?,
            updated_at = now()
        where role_code = ?
        returning role_code, label_zh, side, status
        """,
        (rs, rowNum) -> new RoleRecord(
            rs.getString("role_code"),
            rs.getString("label_zh"),
            rs.getString("side"),
            rs.getString("status")
        ),
        status,
        roleCode
    );
  }

  @Override
  public List<LegacyStatusMappingRecord> listLegacyStatusMappings() {
    return jdbcTemplate.query(
        """
        select legacy_entity_type, legacy_status_text, target_entity_type, target_status,
          migration_mode, quality_score_delta, note
        from legacy_status_mapping
        order by legacy_entity_type, legacy_status_text
        """,
        (rs, rowNum) -> legacyStatusMapping(rs)
    );
  }

  @Override
  public LegacyStatusMappingRecord upsertLegacyStatusMapping(LegacyStatusMappingRecord record) {
    return jdbcTemplate.queryForObject(
        """
        insert into legacy_status_mapping (
          legacy_entity_type, legacy_status_text, target_entity_type, target_status,
          migration_mode, quality_score_delta, note, created_at, updated_at
        ) values (?, ?, ?, ?, ?, ?, ?, now(), now())
        on conflict (legacy_entity_type, legacy_status_text) do update set
          target_entity_type = excluded.target_entity_type,
          target_status = excluded.target_status,
          migration_mode = excluded.migration_mode,
          quality_score_delta = excluded.quality_score_delta,
          note = excluded.note,
          updated_at = now()
        returning legacy_entity_type, legacy_status_text, target_entity_type, target_status,
          migration_mode, quality_score_delta, note
        """,
        (rs, rowNum) -> legacyStatusMapping(rs),
        record.legacyEntityType(),
        record.legacyStatusText(),
        record.targetEntityType(),
        record.targetStatus(),
        record.migrationMode(),
        record.qualityScoreDelta(),
        record.note()
    );
  }

  private HolidayRecord holiday(ResultSet rs) throws SQLException {
    return new HolidayRecord(
        rs.getObject("id", UUID.class),
        rs.getDate("holiday_date").toLocalDate(),
        rs.getString("type"),
        rs.getString("name")
    );
  }

  private OrganizationUnitRecord organizationUnit(ResultSet rs) throws SQLException {
    return new OrganizationUnitRecord(
        rs.getObject("id", UUID.class),
        rs.getObject("parent_id", UUID.class),
        rs.getString("name"),
        rs.getString("status")
    );
  }

  private InternalUserRecord internalUser(ResultSet rs) throws SQLException {
    return new InternalUserRecord(
        rs.getObject("id", UUID.class),
        rs.getString("username"),
        rs.getString("display_name"),
        rs.getString("email"),
        rs.getObject("organization_unit_id", UUID.class),
        rs.getString("status"),
        roleCodes(rs.getArray("role_codes"))
    );
  }

  private LegacyStatusMappingRecord legacyStatusMapping(ResultSet rs) throws SQLException {
    return new LegacyStatusMappingRecord(
        rs.getString("legacy_entity_type"),
        rs.getString("legacy_status_text"),
        rs.getString("target_entity_type"),
        rs.getString("target_status"),
        rs.getString("migration_mode"),
        (Integer) rs.getObject("quality_score_delta"),
        rs.getString("note")
    );
  }

  private List<String> roleCodes(Array sqlArray) throws SQLException {
    if (sqlArray == null) {
      return List.of();
    }
    Object array = sqlArray.getArray();
    if (array instanceof String[] values) {
      return List.of(values);
    }
    if (array instanceof Object[] values) {
      return Arrays.stream(values).map(String::valueOf).toList();
    }
    return List.of();
  }

  private void replaceRoles(UUID userId, List<String> roleCodes) {
    jdbcTemplate.update("delete from user_role where user_id = ?", userId);
    for (String roleCode : roleCodes) {
      jdbcTemplate.update(
          "insert into user_role (user_id, role_code) values (?, ?)",
          userId,
          roleCode
      );
    }
  }
}
