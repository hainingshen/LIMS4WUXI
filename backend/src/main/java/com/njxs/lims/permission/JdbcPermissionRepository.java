package com.njxs.lims.permission;

import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix = "lims.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JdbcPermissionRepository implements PermissionRepository {

  private final JdbcTemplate jdbcTemplate;

  public JdbcPermissionRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public List<PermissionRule> findRules(String actionCode, List<String> roleCodes) {
    if (roleCodes.isEmpty()) {
      return List.of();
    }
    String placeholders = String.join(", ", roleCodes.stream().map(role -> "?").toList());
    Object[] args = new Object[roleCodes.size() + 1];
    args[0] = actionCode;
    for (int i = 0; i < roleCodes.size(); i++) {
      args[i + 1] = roleCodes.get(i);
    }
    return jdbcTemplate.query(
        """
        select action_code, role_code, access, scope
        from permission_matrix
        where action_code = ?
          and role_code in (%s)
        order by role_code
        """.formatted(placeholders),
        (rs, rowNum) -> new PermissionRule(
            rs.getString("action_code"),
            rs.getString("role_code"),
            rs.getString("access"),
            rs.getString("scope")
        ),
        args
    );
  }
}
