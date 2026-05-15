package com.njxs.lims.masterdata.api;

import com.njxs.lims.audit.Audited;
import com.njxs.lims.auth.domain.ClientType;
import com.njxs.lims.common.api.PageResponse;
import com.njxs.lims.masterdata.service.MasterDataAdminService;
import com.njxs.lims.permission.RequiresAction;
import com.njxs.lims.permission.RequiresClient;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
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

  @GetMapping("/roles")
  @RequiresAction("MASTER_DATA_READ")
  public List<RoleResponse> listRoles() {
    return masterDataAdminService.listRoles();
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
