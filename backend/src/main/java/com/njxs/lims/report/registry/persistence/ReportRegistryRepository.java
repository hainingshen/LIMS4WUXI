package com.njxs.lims.report.registry.persistence;

import com.njxs.lims.report.registry.domain.ReportRegistryItemRecord;
import com.njxs.lims.report.registry.domain.ReportRegistryVersionRecord;
import com.njxs.lims.report.registry.service.ReportRegistryQuery;
import java.util.List;
import java.util.UUID;

public interface ReportRegistryRepository {

  List<ReportRegistryItemRecord> search(ReportRegistryQuery query, int limit, int offset);

  int count(ReportRegistryQuery query);

  List<ReportRegistryVersionRecord> versions(List<UUID> reportIds);
}
