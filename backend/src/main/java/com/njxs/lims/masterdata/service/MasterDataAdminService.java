package com.njxs.lims.masterdata.service;

import com.njxs.lims.common.api.PageResponse;
import com.njxs.lims.common.exception.BusinessException;
import com.njxs.lims.auth.domain.AuthPrincipal;
import com.njxs.lims.masterdata.api.HolidayResponse;
import com.njxs.lims.masterdata.api.HolidayUpsertRequest;
import com.njxs.lims.masterdata.api.InternalUserCreateRequest;
import com.njxs.lims.masterdata.api.InternalUserPasswordResetRequest;
import com.njxs.lims.masterdata.api.InternalUserResponse;
import com.njxs.lims.masterdata.api.InternalUserRolesReplaceRequest;
import com.njxs.lims.masterdata.api.InternalUserUpdateRequest;
import com.njxs.lims.masterdata.api.LegacyStatusMappingResponse;
import com.njxs.lims.masterdata.api.LegacyStatusMappingUpsertRequest;
import com.njxs.lims.masterdata.api.OrganizationUnitResponse;
import com.njxs.lims.masterdata.api.RoleResponse;
import com.njxs.lims.masterdata.api.StatusUpdateRequest;
import com.njxs.lims.masterdata.domain.HolidayRecord;
import com.njxs.lims.masterdata.domain.InternalUserRecord;
import com.njxs.lims.masterdata.domain.LegacyStatusMappingRecord;
import com.njxs.lims.masterdata.domain.RoleRecord;
import com.njxs.lims.masterdata.persistence.MasterDataAdminRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(prefix = "lims.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MasterDataAdminService {

  private static final Set<String> HOLIDAY_TYPES = Set.of("HOLIDAY", "WORKDAY");
  private static final Set<String> MIGRATION_MODES = Set.of("ACTIVE_FLOW", "READONLY_ARCHIVE", "MANUAL_REVIEW");
  private static final Set<String> USER_STATUSES = Set.of("ACTIVE", "INACTIVE");
  private static final Set<String> ROLE_STATUSES = Set.of("ACTIVE", "INACTIVE");

  private final MasterDataAdminRepository masterDataAdminRepository;
  private final PasswordEncoder passwordEncoder;

  public MasterDataAdminService(
      MasterDataAdminRepository masterDataAdminRepository,
      PasswordEncoder passwordEncoder
  ) {
    this.masterDataAdminRepository = masterDataAdminRepository;
    this.passwordEncoder = passwordEncoder;
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

  @Transactional
  public InternalUserResponse createInternalUser(InternalUserCreateRequest request, AuthPrincipal principal) {
    String username = requireUsername(request.username());
    if (masterDataAdminRepository.existsUsername(username)) {
      throw new BusinessException(
          "INTERNAL_USER_USERNAME_CONFLICT",
          "内部账号用户名已存在",
          HttpStatus.CONFLICT,
          Map.of("username", username)
      );
    }
    UUID organizationUnitId = requireOrganizationUnit(request.organizationUnitId());
    List<String> roleCodes = normalizeInternalRoles(request.roleCodes());
    InternalUserRecord saved = masterDataAdminRepository.insertInternalUser(
        UUID.randomUUID(),
        username,
        requireText("displayName", request.displayName()),
        normalizeOptionalText(request.email()),
        organizationUnitId,
        passwordEncoder.encode(requireText("password", request.password())),
        roleCodes,
        principal.userId()
    );
    return InternalUserResponse.from(saved);
  }

  @Transactional
  public InternalUserResponse updateInternalUser(
      UUID id,
      InternalUserUpdateRequest request,
      AuthPrincipal principal
  ) {
    requireInternalUser(id);
    UUID organizationUnitId = requireOrganizationUnit(request.organizationUnitId());
    List<String> roleCodes = normalizeInternalRoles(request.roleCodes());
    guardSelfAdminRole(id, roleCodes, principal);
    return InternalUserResponse.from(masterDataAdminRepository.updateInternalUser(
        id,
        requireText("displayName", request.displayName()),
        normalizeOptionalText(request.email()),
        organizationUnitId,
        roleCodes,
        principal.userId()
    ));
  }

  @Transactional
  public InternalUserResponse updateInternalUserStatus(
      UUID id,
      StatusUpdateRequest request,
      AuthPrincipal principal
  ) {
    requireInternalUser(id);
    String status = normalizeStatus("status", request.status(), USER_STATUSES);
    if (principal.userId().equals(id) && "INACTIVE".equals(status)) {
      throw new BusinessException(
          "INTERNAL_USER_SELF_DISABLE_FORBIDDEN",
          "不能停用当前登录账号",
          HttpStatus.CONFLICT,
          Map.of("id", id)
      );
    }
    return InternalUserResponse.from(masterDataAdminRepository.updateInternalUserStatus(id, status, principal.userId()));
  }

  @Transactional
  public InternalUserResponse replaceInternalUserRoles(
      UUID id,
      InternalUserRolesReplaceRequest request,
      AuthPrincipal principal
  ) {
    requireInternalUser(id);
    List<String> roleCodes = normalizeInternalRoles(request.roleCodes());
    guardSelfAdminRole(id, roleCodes, principal);
    return InternalUserResponse.from(masterDataAdminRepository.replaceInternalUserRoles(id, roleCodes, principal.userId()));
  }

  @Transactional
  public InternalUserResponse resetInternalUserPassword(
      UUID id,
      InternalUserPasswordResetRequest request,
      AuthPrincipal principal
  ) {
    requireInternalUser(id);
    return InternalUserResponse.from(masterDataAdminRepository.resetInternalUserPassword(
        id,
        passwordEncoder.encode(requireText("password", request.password())),
        principal.userId()
    ));
  }

  @Transactional
  public RoleResponse updateRoleStatus(String roleCode, StatusUpdateRequest request) {
    String normalizedRoleCode = normalizeCode("roleCode", roleCode);
    RoleRecord existing = masterDataAdminRepository.findRole(normalizedRoleCode)
        .orElseThrow(() -> notFound("role_definition", normalizedRoleCode));
    String status = normalizeStatus("status", request.status(), ROLE_STATUSES);
    if ("SYSTEM_ADMIN".equals(existing.roleCode()) && "INACTIVE".equals(status)) {
      throw new BusinessException(
          "ROLE_SYSTEM_ADMIN_DISABLE_FORBIDDEN",
          "不能停用系统管理员角色",
          HttpStatus.CONFLICT,
          Map.of("roleCode", normalizedRoleCode)
      );
    }
    return RoleResponse.from(masterDataAdminRepository.updateRoleStatus(normalizedRoleCode, status));
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

  private String requireUsername(String value) {
    return requireText("username", value).toLowerCase(Locale.ROOT);
  }

  private UUID requireOrganizationUnit(UUID organizationUnitId) {
    if (organizationUnitId == null) {
      return null;
    }
    if (!masterDataAdminRepository.existsOrganizationUnit(organizationUnitId)) {
      throw notFound("organization_unit", organizationUnitId);
    }
    return organizationUnitId;
  }

  private InternalUserRecord requireInternalUser(UUID id) {
    return masterDataAdminRepository.findInternalUser(id)
        .orElseThrow(() -> notFound("app_user", id));
  }

  private List<String> normalizeInternalRoles(List<String> roleCodes) {
    if (roleCodes == null || roleCodes.isEmpty()) {
      throw validation("roleCodes", "required");
    }
    LinkedHashSet<String> normalized = new LinkedHashSet<>();
    for (String roleCode : roleCodes) {
      normalized.add(normalizeCode("roleCodes", roleCode));
    }
    Set<String> activeRoleCodes = masterDataAdminRepository.activeInternalRoleCodes(List.copyOf(normalized));
    if (!activeRoleCodes.containsAll(normalized)) {
      throw new BusinessException(
          "INTERNAL_USER_ROLE_INVALID",
          "内部账号角色不合法或已停用",
          HttpStatus.BAD_REQUEST,
          Map.of("roleCodes", List.copyOf(normalized))
      );
    }
    return List.copyOf(normalized);
  }

  private String normalizeStatus(String field, String value, Set<String> allowed) {
    String status = normalizeCode(field, value);
    if (!allowed.contains(status)) {
      throw invalid("STATUS_INVALID", "状态不合法", field, status);
    }
    return status;
  }

  private void guardSelfAdminRole(UUID targetUserId, List<String> roleCodes, AuthPrincipal principal) {
    if (principal.userId().equals(targetUserId) && !roleCodes.contains("SYSTEM_ADMIN")) {
      throw new BusinessException(
          "INTERNAL_USER_SELF_ADMIN_ROLE_FORBIDDEN",
          "不能移除当前登录账号的系统管理员角色",
          HttpStatus.CONFLICT,
          Map.of("id", targetUserId)
      );
    }
  }

  private String requireText(String field, String value) {
    String normalized = normalizeOptionalText(value);
    if (normalized == null) {
      throw validation(field, "required");
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

  private BusinessException validation(String field, String reason) {
    return new BusinessException(
        "VALIDATION_FAILED",
        "请求参数校验失败",
        HttpStatus.BAD_REQUEST,
        Map.of(field, reason)
    );
  }

  private BusinessException notFound(String entityType, Object entityId) {
    return new BusinessException(
        "NOT_FOUND",
        "资源不存在",
        HttpStatus.NOT_FOUND,
        Map.of("entityType", entityType, "entityId", entityId)
    );
  }
}
