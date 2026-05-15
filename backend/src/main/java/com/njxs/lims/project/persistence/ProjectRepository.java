package com.njxs.lims.project.persistence;

import com.njxs.lims.project.domain.Project;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository {

  String nextProjectBizNo(int year);

  void save(Project project);

  Optional<Project> findById(UUID id);

  List<Project> list(int limit, int offset);

  int count();

  List<Project> listForCustomerOrg(UUID customerOrgId, int limit, int offset);

  int countForCustomerOrg(UUID customerOrgId);

  List<Project> listForApplicant(UUID applicantUserId, int limit, int offset);

  int countForApplicant(UUID applicantUserId);

  void updateStatus(Project project, UUID actorUserId);

  boolean existsByAssignedSales(UUID projectId, UUID userId);

  boolean existsByTechnicalOwner(UUID projectId, UUID userId);

  boolean existsByBusinessOwner(UUID projectId, UUID userId);

  Optional<UUID> findFirstActiveUserIdByRole(String roleCode);
}
