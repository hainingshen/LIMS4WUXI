package com.njxs.lims.workcenter.api;

import com.njxs.lims.audit.Audited;
import com.njxs.lims.auth.service.AuthContext;
import com.njxs.lims.common.api.PageResponse;
import com.njxs.lims.permission.RequiresAction;
import com.njxs.lims.workcenter.service.WorkCenterBlockerService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/work-center/blockers")
@ConditionalOnProperty(prefix = "lims.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class WorkCenterBlockerController {

  private final WorkCenterBlockerService blockerService;

  public WorkCenterBlockerController(WorkCenterBlockerService blockerService) {
    this.blockerService = blockerService;
  }

  @GetMapping
  @RequiresAction("WORK_CENTER_READ")
  public PageResponse<WorkCenterBlockerResponse> list(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int pageSize
  ) {
    return blockerService.list(AuthContext.require(), page, pageSize);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Audited(actionCode = "WORK_CENTER_BLOCKER_CREATE", entityType = "work_center_blocker")
  @RequiresAction("WORK_CENTER_BLOCKER_MANAGE")
  public WorkCenterBlockerResponse create(@Valid @RequestBody WorkCenterBlockerCreateRequest request) {
    return blockerService.create(request, AuthContext.require());
  }

  @PatchMapping("/{id}")
  @Audited(actionCode = "WORK_CENTER_BLOCKER_UPDATE", entityType = "work_center_blocker")
  @RequiresAction("WORK_CENTER_BLOCKER_MANAGE")
  public WorkCenterBlockerResponse update(
      @PathVariable UUID id,
      @RequestBody WorkCenterBlockerUpdateRequest request
  ) {
    return blockerService.update(id, request, AuthContext.require());
  }

  @PostMapping("/{id}/close")
  @Audited(actionCode = "WORK_CENTER_BLOCKER_CLOSE", entityType = "work_center_blocker")
  @RequiresAction("WORK_CENTER_BLOCKER_MANAGE")
  public WorkCenterBlockerResponse close(
      @PathVariable UUID id,
      @RequestBody WorkCenterBlockerCloseRequest request
  ) {
    return blockerService.close(id, request, AuthContext.require());
  }
}
