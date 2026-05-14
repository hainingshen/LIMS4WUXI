package com.njxs.lims.report.template.persistence;

import com.njxs.lims.report.template.domain.ReportTemplateRecord;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReportTemplateRepository {

  ReportTemplateRecord save(ReportTemplateRecord record, UUID actorUserId);

  Optional<ReportTemplateRecord> findById(UUID id);

  Optional<ReportTemplateRecord> findPublishedByCode(String templateCode);

  List<ReportTemplateRecord> list(int limit, int offset);

  int count();

  boolean existsByCodeAndVersion(String templateCode, String templateVersion);

  boolean fileExists(UUID fileId);

  void deprecatePublishedByCode(String templateCode, UUID exceptId, UUID actorUserId);

  ReportTemplateRecord publish(UUID id, Instant publishedAt, UUID actorUserId);

  ReportTemplateRecord deprecate(UUID id, UUID actorUserId);
}
