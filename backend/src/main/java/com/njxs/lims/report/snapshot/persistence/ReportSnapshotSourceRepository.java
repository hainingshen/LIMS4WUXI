package com.njxs.lims.report.snapshot.persistence;

import com.njxs.lims.report.snapshot.domain.ReportSnapshotSource;
import java.util.Optional;
import java.util.UUID;

public interface ReportSnapshotSourceRepository {

  Optional<ReportSnapshotSource> findByReportId(UUID reportId);
}
