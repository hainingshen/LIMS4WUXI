package com.njxs.lims.change.api;

import com.njxs.lims.audit.Audited;
import com.njxs.lims.auth.service.AuthContext;
import com.njxs.lims.change.service.ChangeRequestService;
import com.njxs.lims.common.api.PageResponse;
import com.njxs.lims.permission.RequiresAction;
import jakarta.validation.Valid;
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
@RequestMapping("/change-requests")
@ConditionalOnProperty(prefix = "lims.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ChangeRequestController {

  private final ChangeRequestService changeRequestService;

  public ChangeRequestController(ChangeRequestService changeRequestService) {
    this.changeRequestService = changeRequestService;
  }

  @GetMapping
  public PageResponse<ChangeRequestResponse> list(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int pageSize
  ) {
    return changeRequestService.list(page, pageSize);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Audited(actionCode = "CHANGE_REQUEST_CREATE", entityType = "change_request")
  @RequiresAction("CHANGE_REQUEST_CREATE")
  public ChangeRequestResponse create(@Valid @RequestBody ChangeRequestCreateRequest request) {
    return changeRequestService.create(request, AuthContext.require());
  }
}
