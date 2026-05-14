package com.njxs.lims.study.persistence;

import com.njxs.lims.study.domain.RawRecordRecord;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RawRecordRepository {

  String nextBizNo(int year);

  RawRecordRecord save(RawRecordRecord record);

  Optional<RawRecordRecord> findById(UUID id);

  List<RawRecordRecord> list(UUID workOrderId, int limit, int offset);

  int count(UUID workOrderId);

  RawRecordRecord updateReview(
      UUID id,
      String status,
      UUID checkerId,
      Instant reviewedAt,
      UUID actorUserId
  );
}
