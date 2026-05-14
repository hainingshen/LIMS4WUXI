package com.njxs.lims.change.persistence;

import com.njxs.lims.change.domain.ChangeRequestRecord;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChangeRequestRepository {

  String nextBizNo(int year);

  ChangeRequestRecord save(ChangeRequestRecord record);

  Optional<ChangeRequestRecord> findById(UUID id);

  List<ChangeRequestRecord> list(int limit, int offset);

  int count();

  ChangeRequestRecord updateStatus(UUID id, String status, UUID actorUserId);

  ChangeRequestRecord markEffective(UUID id, Instant effectiveAt, UUID actorUserId);

  boolean issuedReportExistsForSample(UUID sampleId);
}
