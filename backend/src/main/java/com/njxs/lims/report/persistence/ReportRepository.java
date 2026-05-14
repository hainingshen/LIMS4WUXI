package com.njxs.lims.report.persistence;

import com.njxs.lims.report.domain.ReportRecord;
import com.njxs.lims.report.domain.ReportVersionRecord;
import com.njxs.lims.report.domain.ReportWorkOrderRecord;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface ReportRepository {

  String nextBizNo(int year);

  ReportRecord save(ReportRecord record, UUID actorUserId);

  Optional<ReportRecord> findById(UUID id);

  boolean existsById(UUID id);

  List<ReportRecord> list(int limit, int offset);

  int count();

  List<ReportWorkOrderRecord> findWorkOrders(UUID projectId, List<UUID> workOrderIds);

  boolean workOrdersBelongToProject(UUID projectId, List<UUID> workOrderIds);

  Map<UUID, UUID> findApprovedRawRecordIds(List<UUID> workOrderIds);

  void linkWorkOrders(UUID reportId, Map<UUID, UUID> rawRecordIds);

  UUID saveGeneratedFileMetadata(
      String fileName,
      String objectKey,
      String contentType,
      long sizeBytes,
      String sha256,
      UUID actorUserId
  );

  ReportVersionRecord saveVersion(ReportVersionRecord version, UUID actorUserId);

  ReportRecord updateStatus(UUID id, String status, UUID actorUserId);

  ReportRecord issue(UUID id, UUID actorUserId);

  void updateCurrentVersionStatus(UUID reportId, int versionNo, String status, UUID actorUserId);
}
