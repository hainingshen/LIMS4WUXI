package com.njxs.lims.masterdata.service;

import com.njxs.lims.common.api.PageResponse;
import com.njxs.lims.common.exception.BusinessException;
import com.njxs.lims.masterdata.api.HolidayResponse;
import com.njxs.lims.masterdata.api.HolidayUpsertRequest;
import com.njxs.lims.masterdata.api.InternalUserResponse;
import com.njxs.lims.masterdata.api.LegacyStatusMappingResponse;
import com.njxs.lims.masterdata.api.LegacyStatusMappingUpsertRequest;
import com.njxs.lims.masterdata.api.OrganizationUnitResponse;
import com.njxs.lims.masterdata.api.RoleResponse;
import com.njxs.lims.masterdata.domain.HolidayRecord;
import com.njxs.lims.masterdata.domain.LegacyStatusMappingRecord;
import com.njxs.lims.masterdata.persistence.MasterDataAdminRepository;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "lims.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MasterDataAdminService {

  private static final Set<String> HOLIDAY_TYPES = Set.of("HOLIDAY", "WORKDAY");
  private static final Set<String> MIGRATION_MODES = Set.of("ACTIVE_FLOW", "READONLY_ARCHIVE", "MANUAL_REVIEW");

  private final MasterDataAdminRepository masterDataAdminRepository;

  public MasterDataAdminService(MasterDataAdminRepository masterDataAdminRepository) {
    this.masterDataAdminRepository = masterDataAdminRepository;
  }

  public List<HolidayResponse> listHolidays() {
    return masterDataAdminRepository.listHolidays().stream()
        .map(HolidayResponse::from)
        .toList();
  }

  public HolidayResponse upsertHoliday(HolidayUpsertRequest request) {
    String type = normalizeCode("type", request.type());
    if (!HOLIDAY_TYPES.contains(type)) {
      throw invalid("HOLIDAY_TYPE_INVALID", "节假日类型不合法", "type", type);
    }
    return HolidayResponse.from(masterDataAdminRepository.upsertHoliday(new HolidayRecord(
        request.id() == null ? UUID.randomUUID() : request.id(),
        request.date(),
        type,
        requireText("name", request.name())
    )));
  }

  public List<OrganizationUnitResponse> listOrganizationUnits() {
    return masterDataAdminRepository.listOrganizationUnits().stream()
        .map(OrganizationUnitResponse::from)
        .toList();
  }

  public PageResponse<InternalUserResponse> listInternalUsers(int page, int pageSize) {
    int normalizedPage = Math.max(page, 1);
    int normalizedPageSize = Math.min(Math.max(pageSize, 1), 100);
    int offset = (normalizedPage - 1) * normalizedPageSize;
    return new PageResponse<>(
        masterDataAdminRepository.listInternalUsers(normalizedPageSize, offset).stream()
            .map(InternalUserResponse::from)
            .toList(),
        normalizedPage,
        normalizedPageSize,
        masterDataAdminRepository.countInternalUsers()
    );
  }

  public List<RoleResponse> listRoles() {
    return masterDataAdminRepository.listRoles().stream()
        .map(RoleResponse::from)
        .toList();
  }

  public List<LegacyStatusMappingResponse> listLegacyStatusMappings() {
    return masterDataAdminRepository.listLegacyStatusMappings().stream()
        .map(LegacyStatusMappingResponse::from)
        .toList();
  }

  public LegacyStatusMappingResponse upsertLegacyStatusMapping(LegacyStatusMappingUpsertRequest request) {
    String migrationMode = normalizeCode("migrationMode", request.migrationMode());
    if (!MIGRATION_MODES.contains(migrationMode)) {
      throw invalid("LEGACY_MIGRATION_MODE_INVALID", "旧状态迁移模式不合法", "migrationMode", migrationMode);
    }
    return LegacyStatusMappingResponse.from(masterDataAdminRepository.upsertLegacyStatusMapping(
        new LegacyStatusMappingRecord(
            normalizeCode("legacyEntityType", request.legacyEntityType()),
            requireText("legacyStatusText", request.legacyStatusText()),
            normalizeCode("targetEntityType", request.targetEntityType()),
            normalizeCode("targetStatus", request.targetStatus()),
            migrationMode,
            request.qualityScoreDelta(),
            normalizeOptionalText(request.note())
        )
    ));
  }

  private String normalizeCode(String field, String value) {
    return requireText(field, value).toUpperCase(Locale.ROOT);
  }

  private String requireText(String field, String value) {
    String normalized = normalizeOptionalText(value);
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

  private String normalizeOptionalText(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }

  private BusinessException invalid(String code, String message, String field, String value) {
    return new BusinessException(code, message, HttpStatus.BAD_REQUEST, Map.of(field, value));
  }
}
