package com.njxs.lims.project.api;

import com.njxs.lims.audit.Audited;
import com.njxs.lims.auth.service.AuthContext;
import com.njxs.lims.permission.RequiresAction;
import com.njxs.lims.project.service.NonstandardReviewService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/projects")
@ConditionalOnProperty(prefix = "lims.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ProjectController {

  private final NonstandardReviewService nonstandardReviewService;

  public ProjectController(NonstandardReviewService nonstandardReviewService) {
    this.nonstandardReviewService = nonstandardReviewService;
  }

  @PostMapping("/{id}/nonstandard-review/start")
  @Audited(actionCode = "NONSTANDARD_REVIEW_START", entityType = "project")
  @RequiresAction("NONSTANDARD_REVIEW_START")
  public ProjectResponse startNonstandardReview(@PathVariable UUID id) {
    return nonstandardReviewService.start(id, AuthContext.require());
  }

  @PostMapping("/{projectId}/nonstandard-review/approve")
  @Audited(actionCode = "NONSTANDARD_TECH_APPROVE", entityType = "project")
  public ProjectResponse approveNonstandardReview(
      @PathVariable UUID projectId,
      @Valid @RequestBody NonstandardReviewApprovalRequest request
  ) {
    return nonstandardReviewService.approve(
        projectId,
        request.stage(),
        request.outcome(),
        request.comment(),
        AuthContext.require()
    );
  }
}
