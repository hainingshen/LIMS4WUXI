package com.njxs.lims.project.api;

import com.njxs.lims.audit.Audited;
import com.njxs.lims.auth.domain.AuthPrincipal;
import com.njxs.lims.auth.domain.ClientType;
import com.njxs.lims.auth.service.AuthContext;
import com.njxs.lims.common.api.PageResponse;
import com.njxs.lims.permission.RequiresAction;
import com.njxs.lims.project.persistence.ProjectRepository;
import com.njxs.lims.project.service.NonstandardReviewService;
import com.njxs.lims.quotation.api.QuotationCreateRequest;
import com.njxs.lims.quotation.api.QuotationResponse;
import com.njxs.lims.quotation.service.QuotationService;
import jakarta.validation.Valid;
import java.util.List;
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
@RequestMapping("/projects")
@ConditionalOnProperty(prefix = "lims.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ProjectController {

  private final ProjectRepository projectRepository;
  private final NonstandardReviewService nonstandardReviewService;
  private final QuotationService quotationService;

  public ProjectController(
      ProjectRepository projectRepository,
      NonstandardReviewService nonstandardReviewService,
      QuotationService quotationService
  ) {
    this.projectRepository = projectRepository;
    this.nonstandardReviewService = nonstandardReviewService;
    this.quotationService = quotationService;
  }

  @GetMapping
  public PageResponse<ProjectResponse> list(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int pageSize
  ) {
    int normalizedPage = Math.max(page, 1);
    int normalizedPageSize = Math.min(Math.max(pageSize, 1), 100);
    int offset = (normalizedPage - 1) * normalizedPageSize;
    AuthPrincipal principal = AuthContext.require();
    if (principal.clientType() == ClientType.PORTAL) {
      return listPortalProjects(principal, normalizedPage, normalizedPageSize, offset);
    }
    return new PageResponse<>(
        projectRepository.list(normalizedPageSize, offset).stream().map(ProjectResponse::from).toList(),
        normalizedPage,
        normalizedPageSize,
        projectRepository.count()
    );
  }

  private PageResponse<ProjectResponse> listPortalProjects(
      AuthPrincipal principal,
      int normalizedPage,
      int normalizedPageSize,
      int offset
  ) {
    if (hasRole(principal, "CUSTOMER_APPLICANT")) {
      return new PageResponse<>(
          projectRepository.listForApplicant(principal.userId(), normalizedPageSize, offset)
              .stream()
              .map(ProjectResponse::from)
              .toList(),
          normalizedPage,
          normalizedPageSize,
          projectRepository.countForApplicant(principal.userId())
      );
    }
    if (principal.orgId() != null
        && (hasRole(principal, "CUSTOMER_ORG_ADMIN") || hasRole(principal, "CUSTOMER_VIEWER"))) {
      return new PageResponse<>(
          projectRepository.listForCustomerOrg(principal.orgId(), normalizedPageSize, offset)
              .stream()
              .map(ProjectResponse::from)
              .toList(),
          normalizedPage,
          normalizedPageSize,
          projectRepository.countForCustomerOrg(principal.orgId())
      );
    }
    return new PageResponse<>(List.of(), normalizedPage, normalizedPageSize, 0);
  }

  private boolean hasRole(AuthPrincipal principal, String roleCode) {
    return principal.roles().contains(roleCode);
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
