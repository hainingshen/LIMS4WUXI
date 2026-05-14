package com.njxs.lims.project.api;

import com.njxs.lims.audit.Audited;
import com.njxs.lims.auth.service.AuthContext;
import com.njxs.lims.permission.RequiresAction;
import com.njxs.lims.project.service.NonstandardReviewService;
import com.njxs.lims.quotation.api.QuotationCreateRequest;
import com.njxs.lims.quotation.api.QuotationResponse;
import com.njxs.lims.quotation.service.QuotationService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/projects")
@ConditionalOnProperty(prefix = "lims.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ProjectController {

  private final NonstandardReviewService nonstandardReviewService;
  private final QuotationService quotationService;

  public ProjectController(NonstandardReviewService nonstandardReviewService, QuotationService quotationService) {
    this.nonstandardReviewService = nonstandardReviewService;
    this.quotationService = quotationService;
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

  @PostMapping("/{id}/quotations")
  @ResponseStatus(HttpStatus.CREATED)
  @Audited(actionCode = "QUOTATION_CREATE", entityType = "quotation")
  @RequiresAction("QUOTATION_CREATE")
  public QuotationResponse createQuotation(
      @PathVariable UUID id,
      @Valid @RequestBody QuotationCreateRequest request
  ) {
    return quotationService.create(id, request, AuthContext.require());
  }
}
