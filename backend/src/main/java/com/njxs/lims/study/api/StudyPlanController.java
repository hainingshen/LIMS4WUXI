package com.njxs.lims.study.api;

import com.njxs.lims.audit.Audited;
import com.njxs.lims.auth.domain.ClientType;
import com.njxs.lims.auth.service.AuthContext;
import com.njxs.lims.common.api.PageResponse;
import com.njxs.lims.permission.RequiresAction;
import com.njxs.lims.permission.RequiresClient;
import com.njxs.lims.study.service.StudyPlanService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/study-plans")
@ConditionalOnProperty(prefix = "lims.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class StudyPlanController {

  private final StudyPlanService studyPlanService;

  public StudyPlanController(StudyPlanService studyPlanService) {
    this.studyPlanService = studyPlanService;
  }

  @GetMapping
  @RequiresClient(ClientType.INTERNAL)
  public PageResponse<StudyPlanResponse> list(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int pageSize
  ) {
    return studyPlanService.list(AuthContext.require(), page, pageSize);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Audited(actionCode = "PLAN_CREATE", entityType = "study_plan")
  @RequiresAction("PLAN_CREATE")
  public StudyPlanResponse create(@Valid @RequestBody StudyPlanCreateRequest request) {
    return studyPlanService.create(request, AuthContext.require());
  }

  @PostMapping("/{id}/submit-review")
  @Audited(actionCode = "PLAN_REVIEW_SD", entityType = "study_plan")
  @RequiresAction("PLAN_REVIEW_SD")
  public StudyPlanResponse submitReview(@PathVariable UUID id) {
    return studyPlanService.submitReview(id, AuthContext.require());
  }

  @PostMapping("/{id}/dispatch")
  @Audited(actionCode = "PLAN_DISPATCH", entityType = "study_plan")
  @RequiresAction("PLAN_DISPATCH")
  public StudyPlanResponse dispatch(@PathVariable UUID id) {
    return studyPlanService.dispatch(id, AuthContext.require());
  }
}
