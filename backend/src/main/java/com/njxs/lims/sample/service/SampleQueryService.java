package com.njxs.lims.sample.service;

import com.njxs.lims.common.api.PageResponse;
import com.njxs.lims.sample.api.SampleResponse;
import com.njxs.lims.sample.persistence.SampleRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "lims.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SampleQueryService {

  private final SampleRepository sampleRepository;

  public SampleQueryService(SampleRepository sampleRepository) {
    this.sampleRepository = sampleRepository;
  }

  public PageResponse<SampleResponse> list(String keyword, String status, int page, int pageSize) {
    int normalizedPage = Math.max(page, 1);
    int normalizedPageSize = Math.min(Math.max(pageSize, 1), 100);
    int offset = (normalizedPage - 1) * normalizedPageSize;
    return new PageResponse<>(
        sampleRepository.list(normalize(keyword), normalize(status), normalizedPageSize, offset)
            .stream()
            .map(SampleResponse::from)
            .toList(),
        normalizedPage,
        normalizedPageSize,
        sampleRepository.count(normalize(keyword), normalize(status))
    );
  }

  private String normalize(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }
}
