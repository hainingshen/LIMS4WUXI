package com.njxs.lims.report.delivery.persistence;

import com.njxs.lims.report.delivery.domain.ReportDeliveryLogRecord;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReportDeliveryLogRepository {

  ReportDeliveryLogRecord save(ReportDeliveryLogRecord record, UUID actorUserId);

  Optional<UUID> currentVersionId(UUID reportId);

  List<ReportDeliveryLogRecord> list(UUID reportId, int limit, int offset);

  int count(UUID reportId);
}
