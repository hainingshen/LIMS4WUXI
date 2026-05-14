package com.njxs.lims.workcenter.persistence;

import com.njxs.lims.workcenter.domain.ProjectHealth;
import com.njxs.lims.workcenter.domain.ProjectHealthContext;
import java.util.Optional;
import java.util.UUID;

public interface ProjectHealthRepository {

  Optional<ProjectHealthContext> context(UUID projectId);

  int configInt(String key, int defaultValue);

  void updateHealth(UUID projectId, ProjectHealth health);
}
