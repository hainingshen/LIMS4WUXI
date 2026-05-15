package com.njxs.lims.workcenter.api;

import com.njxs.lims.auth.service.AuthContext;
import com.njxs.lims.common.api.PageResponse;
import com.njxs.lims.permission.RequiresAction;
import com.njxs.lims.workcenter.service.WorkCenterService;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/work-center")
@ConditionalOnProperty(prefix = "lims.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class WorkCenterController {

  private final WorkCenterService workCenterService;

  public WorkCenterController(WorkCenterService workCenterService) {
    this.workCenterService = workCenterService;
  }

  @GetMapping("/summary")
  @RequiresAction("WORK_CENTER_READ")
  public WorkCenterSummaryResponse summary() {
    return workCenterService.summary(AuthContext.require());
  }

  @GetMapping("/projects")
  @RequiresAction("WORK_CENTER_READ")
  public PageResponse<WorkCenterProjectResponse> projects(
      @RequestParam(required = false) String filter,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int pageSize
  ) {
    return workCenterService.projects(AuthContext.require(), filter, page, pageSize);
  }

  @GetMapping("/resource-load")
  @RequiresAction("WORK_CENTER_READ")
  public List<WorkCenterResourceLoadResponse> resourceLoad() {
    return workCenterService.resourceLoad(AuthContext.require());
  }

  @GetMapping("/trends")
  @RequiresAction("WORK_CENTER_READ")
  public List<WorkCenterTrendResponse> trends() {
    return workCenterService.trends(AuthContext.require());
  }

  @GetMapping("/tasks")
  @RequiresAction("WORK_CENTER_READ")
  public List<WorkCenterTaskResponse> tasks(
      @RequestParam(defaultValue = "40") int limit
  ) {
    return workCenterService.tasks(AuthContext.require(), limit);
  }
}
