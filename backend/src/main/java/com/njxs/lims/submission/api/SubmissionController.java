package com.njxs.lims.submission.api;

import com.njxs.lims.audit.Audited;
import com.njxs.lims.auth.service.AuthContext;
import com.njxs.lims.common.api.PageResponse;
import com.njxs.lims.permission.RequiresAction;
import com.njxs.lims.submission.service.SubmissionApplicationService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/submissions")
@ConditionalOnProperty(prefix = "lims.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SubmissionController {

  private final SubmissionApplicationService submissionApplicationService;

  public SubmissionController(SubmissionApplicationService submissionApplicationService) {
    this.submissionApplicationService = submissionApplicationService;
  }

  @GetMapping
  public PageResponse<SubmissionResponse> list(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int pageSize
  ) {
    return submissionApplicationService.list(AuthContext.require(), page, pageSize);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Audited(actionCode = "SUBMISSION_CREATE", entityType = "submission")
  @RequiresAction("SUBMISSION_CREATE")
  public SubmissionResponse create(@Valid @RequestBody SubmissionDraftRequest request) {
    return submissionApplicationService.createDraft(request, AuthContext.require());
  }

  @PostMapping("/{id}/submit")
  @Audited(actionCode = "SUBMISSION_CREATE", entityType = "submission")
  @RequiresAction("SUBMISSION_CREATE")
  public SubmissionResponse submit(@PathVariable UUID id) {
    return submissionApplicationService.submit(id, AuthContext.require());
  }

  @PostMapping("/{id}/accept")
  @Audited(actionCode = "SUBMISSION_ACCEPT", entityType = "submission")
  @RequiresAction("SUBMISSION_ACCEPT")
  public SubmissionResponse accept(@PathVariable UUID id) {
    return submissionApplicationService.accept(id, AuthContext.require());
  }
}
