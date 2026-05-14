package com.njxs.lims.project.persistence;

import com.njxs.lims.project.domain.Project;

public interface ProjectRepository {

  String nextProjectBizNo(int year);

  void save(Project project);
}
