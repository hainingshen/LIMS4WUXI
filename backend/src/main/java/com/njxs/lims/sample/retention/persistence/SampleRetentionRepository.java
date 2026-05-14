package com.njxs.lims.sample.retention.persistence;

import com.njxs.lims.sample.retention.domain.SampleRetentionRecord;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SampleRetentionRepository {

  String nextBizNo(int year);

  SampleRetentionRecord save(SampleRetentionRecord record);

  Optional<SampleRetentionRecord> findById(UUID id);

  List<SampleRetentionRecord> list(int limit, int offset);

  int count();

  SampleRetentionRecord updateStatus(UUID id, String status, UUID actorUserId);

  SampleRetentionRecord extend(UUID id, LocalDate retainUntil, String reason, UUID actorUserId);

  SampleRetentionRecord dispose(
      UUID id,
      String disposition,
      String status,
      String dispositionReason,
      UUID proofFileId,
      UUID actorUserId
  );

  boolean paymentUnlocked(UUID id);
}
