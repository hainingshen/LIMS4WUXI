package com.njxs.lims.common.persistence;

import com.njxs.lims.common.domain.BaseEntity;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Objects;

public final class BaseEntityJdbcSupport {

  private BaseEntityJdbcSupport() {
  }

  public static void prepareForInsert(BaseEntity entity) {
    Objects.requireNonNull(entity.getId(), "id is required");
    Instant now = Instant.now();
    if (entity.getCreatedAt() == null) {
      entity.setCreatedAt(now);
    }
    if (entity.getUpdatedAt() == null) {
      entity.setUpdatedAt(entity.getCreatedAt());
    }
    if (entity.getVersion() == null) {
      entity.setVersion(0);
    }
  }

  public static Timestamp timestamp(Instant instant) {
    if (instant == null) {
      return null;
    }
    return Timestamp.from(instant);
  }
}
