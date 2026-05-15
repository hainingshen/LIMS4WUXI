package com.njxs.lims.archive.service;

import com.njxs.lims.archive.api.ArchiveItemCreateRequest;
import com.njxs.lims.archive.api.ArchiveItemResponse;
import com.njxs.lims.archive.domain.ArchiveItemRecord;
import com.njxs.lims.archive.persistence.ArchiveItemRepository;
import com.njxs.lims.auth.domain.AuthPrincipal;
import com.njxs.lims.common.api.PageResponse;
import com.njxs.lims.common.exception.BusinessException;
import com.njxs.lims.permission.InternalDataScope;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "lims.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ArchiveItemService {

  private final ArchiveItemRepository archiveItemRepository;

  public ArchiveItemService(ArchiveItemRepository archiveItemRepository) {
    this.archiveItemRepository = archiveItemRepository;
  }

  public PageResponse<ArchiveItemResponse> list(AuthPrincipal principal, UUID projectId, int page, int pageSize) {
    int normalizedPage = Math.max(page, 1);
    int normalizedPageSize = Math.min(Math.max(pageSize, 1), 100);
    int offset = (normalizedPage - 1) * normalizedPageSize;
    InternalDataScope scope = InternalDataScope.from(principal);
    return new PageResponse<>(
        archiveItemRepository.listVisibleTo(scope, projectId, normalizedPageSize, offset).stream()
            .map(ArchiveItemResponse::from)
            .toList(),
        normalizedPage,
        normalizedPageSize,
        archiveItemRepository.countVisibleTo(scope, projectId)
    );
  }

  public ArchiveItemResponse create(ArchiveItemCreateRequest request, AuthPrincipal principal) {
    if (!archiveItemRepository.existsProject(request.projectId())) {
      throw new BusinessException(
          "NOT_FOUND",
          "资源不存在",
          HttpStatus.NOT_FOUND,
          Map.of("entityType", "project", "entityId", request.projectId())
      );
    }
    if (!archiveItemRepository.existsFile(request.fileId())) {
      throw new BusinessException(
          "FILE_NOT_FOUND",
          "文件不存在",
          HttpStatus.CONFLICT,
          Map.of("fileId", request.fileId())
      );
    }
    Instant now = Instant.now();
    ArchiveItemRecord record = new ArchiveItemRecord(
        UUID.randomUUID(),
        request.projectId(),
        normalizeOptionalCode(request.entityType()),
        request.entityId(),
        normalizeRequiredCode("archiveType", request.archiveType()),
        request.fileId(),
        "ARCHIVED",
        now,
        principal.userId(),
        null,
        null,
        null,
        null,
        null,
        null
    );
    return ArchiveItemResponse.from(archiveItemRepository.save(record));
  }

  private String normalizeRequiredCode(String field, String value) {
    String normalized = normalizeOptionalCode(value);
    if (normalized == null) {
      throw new BusinessException(
          "VALIDATION_FAILED",
          "请求参数校验失败",
          HttpStatus.BAD_REQUEST,
          Map.of(field, "required")
      );
    }
    return normalized;
  }

  private String normalizeOptionalCode(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim().toUpperCase(Locale.ROOT);
  }
}
