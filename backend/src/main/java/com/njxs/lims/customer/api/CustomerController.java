package com.njxs.lims.customer.api;

import com.njxs.lims.audit.Audited;
import com.njxs.lims.auth.domain.ClientType;
import com.njxs.lims.auth.service.AuthContext;
import com.njxs.lims.common.api.PageResponse;
import com.njxs.lims.customer.service.CustomerManagementService;
import com.njxs.lims.permission.RequiresAction;
import com.njxs.lims.permission.RequiresClient;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnProperty(prefix = "lims.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CustomerController {

  private final CustomerManagementService customerManagementService;

  public CustomerController(CustomerManagementService customerManagementService) {
    this.customerManagementService = customerManagementService;
  }

  @GetMapping("/customer-orgs")
  @RequiresClient(ClientType.INTERNAL)
  @RequiresAction("MASTER_DATA_READ")
  public PageResponse<CustomerOrgResponse> listCustomerOrgs(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int pageSize,
      @RequestParam(required = false) String keyword
  ) {
    return customerManagementService.listCustomerOrgs(keyword, page, pageSize);
  }

  @PostMapping("/customer-orgs")
  @Audited(actionCode = "MASTER_DATA_EDIT", entityType = "customer_org")
  @RequiresClient(ClientType.INTERNAL)
  @RequiresAction("MASTER_DATA_EDIT")
  public CustomerOrgResponse upsertCustomerOrg(@Valid @RequestBody CustomerOrgUpsertRequest request) {
    return customerManagementService.upsertCustomerOrg(request, AuthContext.require());
  }

  @GetMapping("/portal-users")
  @RequiresClient(ClientType.INTERNAL)
  @RequiresAction("MASTER_DATA_READ")
  public PageResponse<PortalUserResponse> listPortalUsers(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int pageSize,
      @RequestParam(required = false) UUID customerOrgId
  ) {
    return customerManagementService.listPortalUsers(customerOrgId, page, pageSize);
  }

  @PostMapping("/portal-users")
  @ResponseStatus(HttpStatus.CREATED)
  @Audited(actionCode = "MASTER_DATA_EDIT", entityType = "portal_user")
  @RequiresClient(ClientType.INTERNAL)
  @RequiresAction("MASTER_DATA_EDIT")
  public PortalUserResponse createPortalUser(@Valid @RequestBody PortalUserCreateRequest request) {
    return customerManagementService.createPortalUser(request, AuthContext.require());
  }
}
