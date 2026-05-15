package com.njxs.lims.masterdata.api;

import com.njxs.lims.audit.Audited;
import com.njxs.lims.audit.api.AuditLogResponse;
import com.njxs.lims.auth.domain.ClientType;
import com.njxs.lims.common.api.PageResponse;
import com.njxs.lims.masterdata.service.SystemGovernanceService;
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
public class SystemGovernanceController {

  private final SystemGovernanceService systemGovernanceService;

  public SystemGovernanceController(SystemGovernanceService systemGovernanceService) {
    this.systemGovernanceService = systemGovernanceService;
  }

  @GetMapping("/system-configs")
  @RequiresAction("MASTER_DATA_READ")
  public List<SystemConfigResponse> listSystemConfigs() {
    return systemGovernanceService.listSystemConfigs();
  }

  @PostMapping("/system-configs")
  @Audited(actionCode = "SYSTEM_CONFIG", entityType = "system_config")
  @RequiresAction("SYSTEM_CONFIG")
  public SystemConfigResponse upsertSystemConfig(@Valid @RequestBody SystemConfigUpsertRequest request) {
    return systemGovernanceService.upsertSystemConfig(request);
  }

  @GetMapping("/business-rules")
  @RequiresAction("MASTER_DATA_READ")
  public List<BusinessRuleResponse> listBusinessRules() {
    return systemGovernanceService.listBusinessRules();
  }

  @PostMapping("/business-rules")
  @Audited(actionCode = "SYSTEM_CONFIG", entityType = "business_rule")
  @RequiresAction("SYSTEM_CONFIG")
  public BusinessRuleResponse upsertBusinessRule(@Valid @RequestBody BusinessRuleUpsertRequest request) {
    return systemGovernanceService.upsertBusinessRule(request);
  }

  @GetMapping("/audit-logs")
  @RequiresAction("AUDIT_LOG_READ")
  public PageResponse<AuditLogResponse> listAuditLogs(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int pageSize
  ) {
    return systemGovernanceService.listAuditLogs(page, pageSize);
  }
}
