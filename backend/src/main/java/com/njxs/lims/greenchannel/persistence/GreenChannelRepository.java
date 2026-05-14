package com.njxs.lims.greenchannel.persistence;

import com.njxs.lims.greenchannel.domain.GreenChannelRequestRecord;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GreenChannelRepository {

  String nextBizNo(int year);

  GreenChannelRequestRecord save(GreenChannelRequestRecord record, UUID actorUserId);

  Optional<GreenChannelRequestRecord> findById(UUID id);

  List<GreenChannelRequestRecord> list(UUID projectId, int limit, int offset);

  int count(UUID projectId);

  boolean reportBelongsToProject(UUID reportId, UUID projectId);

  boolean reportAssignedSales(UUID reportId, UUID userId);

  boolean existsById(UUID id);

  GreenChannelRequestRecord updateStatus(UUID id, String status, UUID actorUserId);

  void greenUnlockReport(UUID reportId, UUID actorUserId);
}
