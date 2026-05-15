package com.njxs.lims.study.persistence;

import com.njxs.lims.permission.InternalDataScope;
import com.njxs.lims.study.domain.StudyPlanRecord;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudyPlanRepository {

  String nextPlanBizNo(int year);

  String nextWorkOrderBizNo(int year);

  StudyPlanRecord save(StudyPlanRecord record, UUID actorUserId);

  Optional<StudyPlanRecord> findById(UUID id);

  List<StudyPlanRecord> list(int limit, int offset);

  int count();

  List<StudyPlanRecord> listVisibleTo(InternalDataScope scope, int limit, int offset);

  int countVisibleTo(InternalDataScope scope);

  StudyPlanRecord markReviewed(UUID id, String status, Instant reviewedAt, UUID actorUserId);

  StudyPlanRecord updateStatus(UUID id, String status, UUID actorUserId);

  int createWorkOrders(UUID studyPlanId, UUID actorUserId);

  int workOrderCount(UUID studyPlanId);

  boolean signedContractExists(UUID projectId);
}
