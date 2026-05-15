package com.njxs.lims.auth.persistence;

import com.njxs.lims.auth.domain.AuthUser;
import com.njxs.lims.auth.domain.ClientType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix = "lims.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JdbcAuthUserRepository implements AuthUserRepository {

  private final JdbcTemplate jdbcTemplate;

  public JdbcAuthUserRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public Optional<AuthUser> findActiveByUsernameAndClientType(String username, ClientType clientType) {
    List<AuthUser> users = jdbcTemplate.query(
        """
        select id, username, display_name, password_hash, client_type, customer_org_id
        from app_user
        where username = ?
          and client_type = ?
          and status = 'ACTIVE'
          and deleted_at is null
        """,
        (rs, rowNum) -> user(rs),
        username,
        clientType.name()
    );
    return users.stream().findFirst().map(this::withRoles);
  }

  @Override
  public Optional<AuthUser> findActiveByIdAndClientType(UUID userId, ClientType clientType) {
    List<AuthUser> users = jdbcTemplate.query(
        """
        select id, username, display_name, password_hash, client_type, customer_org_id
        from app_user
        where id = ?
          and client_type = ?
          and status = 'ACTIVE'
          and deleted_at is null
        """,
        (rs, rowNum) -> user(rs),
        userId,
        clientType.name()
    );
    return users.stream().findFirst().map(this::withRoles);
  }

  @Override
  public List<String> findDataScopes(List<String> roleCodes) {
    if (roleCodes.isEmpty()) {
      return List.of();
    }
    String placeholders = String.join(", ", roleCodes.stream().map(role -> "?").toList());
    return jdbcTemplate.queryForList(
        "select distinct scope from permission_matrix where role_code in (" + placeholders + ") order by scope",
        String.class,
        roleCodes.toArray()
    );
  }

  private AuthUser user(ResultSet rs) throws SQLException {
    return new AuthUser(
        rs.getObject("id", UUID.class),
        rs.getString("username"),
        rs.getString("display_name"),
        rs.getString("password_hash"),
        ClientType.valueOf(rs.getString("client_type")),
        rs.getObject("customer_org_id", UUID.class),
        List.of()
    );
  }

  private AuthUser withRoles(AuthUser user) {
    List<String> roles = jdbcTemplate.queryForList(
        """
        select ur.role_code
        from user_role ur
        join role_definition rd on rd.role_code = ur.role_code
        where ur.user_id = ?
          and rd.status = 'ACTIVE'
        order by ur.role_code
        """,
        String.class,
        user.id()
    );
    return new AuthUser(
        user.id(),
        user.username(),
        user.displayName(),
        user.passwordHash(),
        user.clientType(),
        user.customerOrgId(),
        roles
    );
  }
}
