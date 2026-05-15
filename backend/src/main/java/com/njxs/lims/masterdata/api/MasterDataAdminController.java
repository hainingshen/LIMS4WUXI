package com.njxs.lims.masterdata.api;

import com.njxs.lims.audit.Audited;
import com.njxs.lims.auth.domain.ClientType;
import com.njxs.lims.auth.service.AuthContext;
import com.njxs.lims.common.api.PageResponse;
import com.njxs.lims.masterdata.service.MasterDataAdminService;
import com.njxs.lims.permission.RequiresAction;
import com.njxs.lims.permission.RequiresClient;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiresClient(ClientType.INTERNAL)
@ConditionalOnProperty(prefix = "lims.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MasterDataAdminController {

  private final MasterDataAdminService masterDataAdminService;

  public MasterDataAdminController(MasterDataAdminService masterDataAdminService) {
    this.masterDataAdminService = masterDataAdminService;
  }

  @GetMapping("/holidays")
  @RequiresAction("MASTER_DATA_READ")
  public List<HolidayResponse> listHolidays() {
    return masterDataAdminService.listHolidays();
  }

  @PostMapping("/holidays")
  @Audited(actionCode = "HOLIDAY_EDIT", entityType = "holiday")
  @RequiresAction("HOLIDAY_EDIT")
  public HolidayResponse upsertHoliday(@Valid @RequestBody HolidayUpsertRequest request) {
    return masterDataAdminService.upsertHoliday(request);
  }

  @GetMapping("/organization-units")
  @RequiresAction("MASTER_DATA_READ")
  public List<OrganizationUnitResponse> listOrganizationUnits() {
    return masterDataAdminService.listOrganizationUnits();
  }

  @GetMapping("/users")
  @RequiresAction("MASTER_DATA_READ")
  public PageResponse<InternalUserResponse> listInternalUsers(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int pageSize
  ) {
    return masterDataAdminService.listInternalUsers(page, pageSize);
  }

  @PostMapping("/users")
  @ResponseStatus(HttpStatus.CREATED)
  @Audited(actionCode = "USER_CREATE", entityType = "app_user")
  @RequiresAction("MASTER_DATA_EDIT")
  public InternalUserResponse createInternalUser(@Valid @RequestBody InternalUserCreateRequest request) {
    return masterDataAdminService.createInternalUser(request, AuthContext.require());
  }

  @PatchMapping("/users/{id}")
  @Audited(actionCode = "USER_UPDATE", entityType = "app_user")
  @RequiresAction("MASTER_DATA_EDIT")
  public InternalUserResponse updateInternalUser(
      @PathVariable UUID id,
      @Valid @RequestBody InternalUserUpdateRequest request
  ) {
    return masterDataAdminService.updateInternalUser(id, request, AuthContext.require());
  }

  @PostMapping("/users/{id}/status")
  @Audited(actionCode = "USER_STATUS_UPDATE", entityType = "app_user")
  @RequiresAction("MASTER_DATA_EDIT")
  public InternalUserResponse updateInternalUserStatus(
      @PathVariable UUID id,
      @Valid @RequestBody StatusUpdateRequest request
  ) {
    return masterDataAdminService.updateInternalUserStatus(id, request, AuthContext.require());
  }

  @PostMapping("/users/{id}/roles")
  @Audited(actionCode = "USER_ROLE_REPLACE", entityType = "app_user")
  @RequiresAction("MASTER_DATA_EDIT")
  public InternalUserResponse replaceInternalUserRoles(
      @PathVariable UUID id,
      @Valid @RequestBody InternalUserRolesReplaceRequest request
  ) {
    return masterDataAdminService.replaceInternalUserRoles(id, request, AuthContext.require());
  }

  @PostMapping("/users/{id}/reset-password")
  @Audited(actionCode = "USER_PASSWORD_RESET", entityType = "app_user")
  @RequiresAction("MASTER_DATA_EDIT")
  public InternalUserResponse resetInternalUserPassword(
      @PathVariable UUID id,
      @Valid @RequestBody InternalUserPasswordResetRequest request
  ) {
    return masterDataAdminService.resetInternalUserPassword(id, request, AuthContext.require());
  }

  @GetMapping("/roles")
  @RequiresAction("MASTER_DATA_READ")
  public List<RoleResponse> listRoles() {
    return masterDataAdminService.listRoles();
  }

  @PostMapping("/roles/{roleCode}/status")
  @Audited(actionCode = "ROLE_STATUS_UPDATE", entityType = "role_definition")
  @RequiresAction("MASTER_DATA_EDIT")
  public RoleResponse updateRoleStatus(
      @PathVariable String roleCode,
      @Valid @RequestBody StatusUpdateRequest request
  ) {
    return masterDataAdminService.updateRoleStatus(roleCode, request);
  }

  @GetMapping("/legacy-status-mappings")
  @RequiresAction("MASTER_DATA_READ")
  public List<LegacyStatusMappingResponse> listLegacyStatusMappings() {
    return masterDataAdminService.listLegacyStatusMappings();
  }

  @PostMapping("/legacy-status-mappings")
  @Audited(actionCode = "MASTER_DATA_EDIT", entityType = "legacy_status_mapping")
  @RequiresAction("MASTER_DATA_EDIT")
  public LegacyStatusMappingResponse upsertLegacyStatusMapping(
      @Valid @RequestBody LegacyStatusMappingUpsertRequest request
  ) {
    return masterDataAdminService.upsertLegacyStatusMapping(request);
  }
}
