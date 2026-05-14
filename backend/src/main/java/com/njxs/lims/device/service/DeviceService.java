package com.njxs.lims.device.service;

import com.njxs.lims.auth.domain.AuthPrincipal;
import com.njxs.lims.common.api.PageResponse;
import com.njxs.lims.common.exception.BusinessException;
import com.njxs.lims.device.api.DeviceFileResponse;
import com.njxs.lims.device.api.DeviceFileUploadRequest;
import com.njxs.lims.device.api.DeviceResponse;
import com.njxs.lims.device.domain.DeviceFileRecord;
import com.njxs.lims.device.persistence.DeviceRepository;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(prefix = "lims.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DeviceService {

  private static final Set<String> FILE_TYPES = Set.of(
      "MANUAL",
      "CALIBRATION_CERT",
      "VERIFICATION_CERT",
      "MAINTENANCE_RECORD",
      "SOP",
      "TRAINING_RECORD",
      "OTHER"
  );

  private final DeviceRepository deviceRepository;

  public DeviceService(DeviceRepository deviceRepository) {
    this.deviceRepository = deviceRepository;
  }

  public PageResponse<DeviceResponse> list(String keyword, int page, int pageSize) {
    int normalizedPage = Math.max(page, 1);
    int normalizedPageSize = Math.min(Math.max(pageSize, 1), 100);
    int offset = (normalizedPage - 1) * normalizedPageSize;
    return new PageResponse<>(
        deviceRepository.list(keyword, normalizedPageSize, offset).stream()
            .map(DeviceResponse::from)
            .toList(),
        normalizedPage,
        normalizedPageSize,
        deviceRepository.count(keyword)
    );
  }

  public java.util.List<DeviceFileResponse> listFiles(UUID deviceId) {
    requireDevice(deviceId);
    return deviceRepository.listFiles(deviceId).stream()
        .map(DeviceFileResponse::from)
        .toList();
  }

  @Transactional
  public DeviceFileResponse uploadFile(UUID deviceId, DeviceFileUploadRequest request, AuthPrincipal principal) {
    requireDevice(deviceId);
    if (!deviceRepository.existsFile(request.fileId())) {
      throw new BusinessException(
          "FILE_NOT_FOUND",
          "文件不存在",
          HttpStatus.CONFLICT,
          Map.of("fileId", request.fileId())
      );
    }
    String fileType = requireFileType(request.fileType());
    if (request.currentOrDefault()) {
      deviceRepository.clearCurrent(deviceId, fileType, principal.userId());
    }
    DeviceFileRecord record = new DeviceFileRecord(
        UUID.randomUUID(),
        deviceId,
        fileType,
        request.fileId(),
        emptyToNull(request.versionNo()),
        request.validFrom(),
        request.validUntil(),
        request.currentOrDefault(),
        principal.userId(),
        Instant.now(),
        "ACTIVE"
    );
    return DeviceFileResponse.from(deviceRepository.saveFile(record, principal.userId()));
  }

  @Transactional
  public DeviceFileResponse disableFile(UUID id, AuthPrincipal principal) {
    return deviceRepository.disableFile(id, principal.userId())
        .map(DeviceFileResponse::from)
        .orElseThrow(() -> new BusinessException(
            "NOT_FOUND",
            "资源不存在",
            HttpStatus.NOT_FOUND,
            Map.of("entityType", "device_file", "entityId", id)
        ));
  }

  private void requireDevice(UUID deviceId) {
    if (!deviceRepository.existsDevice(deviceId)) {
      throw new BusinessException(
          "NOT_FOUND",
          "资源不存在",
          HttpStatus.NOT_FOUND,
          Map.of("entityType", "device", "entityId", deviceId)
      );
    }
  }

  private String requireFileType(String value) {
    String fileType = value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    if (!FILE_TYPES.contains(fileType)) {
      throw new BusinessException(
          "DEVICE_FILE_TYPE_INVALID",
          "设备文件类型不合法",
          HttpStatus.BAD_REQUEST,
          Map.of("fileType", String.valueOf(value))
      );
    }
    return fileType;
  }

  private String emptyToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
