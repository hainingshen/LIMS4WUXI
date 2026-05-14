package com.njxs.lims.report.registry.service;

import com.njxs.lims.common.api.PageResponse;
import com.njxs.lims.report.registry.api.ReportRegistryItemResponse;
import com.njxs.lims.report.registry.domain.ReportRegistryItemRecord;
import com.njxs.lims.report.registry.domain.ReportRegistryVersionRecord;
import com.njxs.lims.report.registry.persistence.ReportRegistryRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "lims.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ReportRegistryService {

  private final ReportRegistryRepository reportRegistryRepository;

  public ReportRegistryService(ReportRegistryRepository reportRegistryRepository) {
    this.reportRegistryRepository = reportRegistryRepository;
  }

  public PageResponse<ReportRegistryItemResponse> search(ReportRegistryQuery query, int page, int pageSize) {
    int normalizedPage = Math.max(page, 1);
    int normalizedPageSize = Math.min(Math.max(pageSize, 1), 100);
    int offset = (normalizedPage - 1) * normalizedPageSize;
    List<ReportRegistryItemRecord> items = reportRegistryRepository.search(
        query,
        normalizedPageSize,
        offset
    );
    List<ReportRegistryItemRecord> withVersions = attachVersions(items);
    return new PageResponse<>(
        withVersions.stream()
            .map(ReportRegistryItemResponse::from)
            .toList(),
        normalizedPage,
        normalizedPageSize,
        reportRegistryRepository.count(query)
    );
  }

  private List<ReportRegistryItemRecord> attachVersions(List<ReportRegistryItemRecord> items) {
    List<UUID> reportIds = items.stream()
        .map(ReportRegistryItemRecord::reportId)
        .toList();
    Map<UUID, List<ReportRegistryVersionRecord>> versionsByReport = new LinkedHashMap<>();
    for (ReportRegistryVersionRecord version : reportRegistryRepository.versions(reportIds)) {
      versionsByReport.computeIfAbsent(version.reportId(), ignored -> new ArrayList<>()).add(version);
    }
    return items.stream()
        .map(item -> item.withVersions(versionsByReport.getOrDefault(item.reportId(), List.of())))
        .toList();
  }
}
