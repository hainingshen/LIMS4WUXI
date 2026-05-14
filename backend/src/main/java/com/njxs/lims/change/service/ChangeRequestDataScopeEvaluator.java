package com.njxs.lims.change.service;

import com.njxs.lims.auth.domain.AuthPrincipal;
import com.njxs.lims.change.api.ChangeRequestCreateRequest;
import com.njxs.lims.change.domain.ChangeRequestRecord;
import com.njxs.lims.change.persistence.ChangeRequestRepository;
import com.njxs.lims.contract.persistence.ContractRepository;
import com.njxs.lims.permission.DataScopeEvaluator;
import com.njxs.lims.project.persistence.ProjectRepository;
import com.njxs.lims.sample.domain.Sample;
import com.njxs.lims.sample.persistence.SampleRepository;
import com.njxs.lims.submission.persistence.SubmissionRepository;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "lims.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ChangeRequestDataScopeEvaluator implements DataScopeEvaluator {

  private static final String ACTION_CREATE = "CHANGE_REQUEST_CREATE";
  private static final String ACTION_SUBMIT = "CHANGE_REQUEST_SUBMIT";
  private static final String ACTION_APPROVE = "CHANGE_REQUEST_APPROVE";
  private static final String ACTION_EFFECT = "CHANGE_REQUEST_EFFECT";
  private static final Set<String> CREATE_SCOPES = Set.of(
      "ASSIGNED_PROJECT",
      "SAMPLE_SCOPE",
      "REPORT_SCOPE",
      "TECH_SCOPE"
  );

  private final SubmissionRepository submissionRepository;
  private final ProjectRepository projectRepository;
  private final SampleRepository sampleRepository;
  private final ContractRepository contractRepository;
  private final ChangeRequestRepository changeRequestRepository;
  private final JdbcTemplate jdbcTemplate;

  public ChangeRequestDataScopeEvaluator(
      SubmissionRepository submissionRepository,
      ProjectRepository projectRepository,
      SampleRepository sampleRepository,
      ContractRepository contractRepository,
      ChangeRequestRepository changeRequestRepository,
      JdbcTemplate jdbcTemplate
  ) {
    this.submissionRepository = submissionRepository;
    this.projectRepository = projectRepository;
    this.sampleRepository = sampleRepository;
    this.contractRepository = contractRepository;
    this.changeRequestRepository = changeRequestRepository;
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public boolean supports(String actionCode, String scope) {
    return (ACTION_CREATE.equals(actionCode) && CREATE_SCOPES.contains(scope))
        || (ACTION_SUBMIT.equals(actionCode) && "OWN_REQUEST".equals(scope))
        || (ACTION_APPROVE.equals(actionCode) && "TASK".equals(scope))
        || (ACTION_EFFECT.equals(actionCode) && "APPROVED_REQUEST".equals(scope));
  }

  @Override
  public boolean isAllowed(AuthPrincipal principal, String actionCode, String scope, Object[] args) {
    if (ACTION_SUBMIT.equals(actionCode)) {
      return firstUuid(args)
          .flatMap(changeRequestRepository::findById)
          .map(record -> principal.userId().equals(record.requestedBy()))
          .orElse(false);
    }
    if (ACTION_APPROVE.equals(actionCode)) {
      return firstUuid(args).flatMap(changeRequestRepository::findById).isPresent();
    }
    if (ACTION_EFFECT.equals(actionCode)) {
      return firstUuid(args)
          .flatMap(changeRequestRepository::findById)
          .map(ChangeRequestRecord::status)
          .map("APPROVED"::equals)
          .orElse(false);
    }
    Optional<ChangeRequestCreateRequest> request = request(args);
    if (request.isEmpty()) {
      return false;
    }
    return switch (scope) {
      case "ASSIGNED_PROJECT" -> isAssignedProject(principal, request.get());
      case "SAMPLE_SCOPE" -> isSampleScope(request.get());
      case "REPORT_SCOPE" -> isReportScope(request.get());
      case "TECH_SCOPE" -> isTechScope(principal, request.get());
      default -> false;
    };
  }

  private boolean isAssignedProject(AuthPrincipal principal, ChangeRequestCreateRequest request) {
    String targetType = targetType(request);
    return switch (targetType) {
      case "SUBMISSION" -> submissionRepository.existsByAssignedSales(request.targetId(), principal.userId());
      case "PROJECT" -> projectRepository.existsByAssignedSales(request.targetId(), principal.userId());
      case "CONTRACT" -> contractRepository.existsByAssignedSales(request.targetId(), principal.userId());
      case "SAMPLE" -> sampleRepository.findById(request.targetId())
          .map(Sample::getProjectId)
          .map(projectId -> projectRepository.existsByAssignedSales(projectId, principal.userId()))
          .orElse(false);
      case "REPORT" -> reportProjectId(request.targetId())
          .map(projectId -> projectRepository.existsByAssignedSales(projectId, principal.userId()))
          .orElse(false);
      default -> false;
    };
  }

  private boolean isSampleScope(ChangeRequestCreateRequest request) {
    return "SAMPLE".equals(targetType(request)) && sampleRepository.findById(request.targetId()).isPresent();
  }

  private boolean isReportScope(ChangeRequestCreateRequest request) {
    return "REPORT".equals(targetType(request)) && reportProjectId(request.targetId()).isPresent();
  }

  private boolean isTechScope(AuthPrincipal principal, ChangeRequestCreateRequest request) {
    return projectId(request)
        .map(projectId -> projectRepository.existsByTechnicalOwner(projectId, principal.userId()))
        .orElse(false);
  }

  private Optional<UUID> projectId(ChangeRequestCreateRequest request) {
    String targetType = targetType(request);
    return switch (targetType) {
      case "PROJECT" -> Optional.of(request.targetId());
      case "SAMPLE" -> sampleRepository.findById(request.targetId()).map(Sample::getProjectId);
      case "REPORT" -> reportProjectId(request.targetId());
      default -> Optional.empty();
    };
  }

  private Optional<UUID> reportProjectId(UUID reportId) {
    return jdbcTemplate.query(
        "select project_id from report where id = ? and deleted_at is null",
        (rs, rowNum) -> rs.getObject("project_id", UUID.class),
        reportId
    ).stream().findFirst();
  }

  private Optional<ChangeRequestCreateRequest> request(Object[] args) {
    return Arrays.stream(args)
        .filter(ChangeRequestCreateRequest.class::isInstance)
        .map(ChangeRequestCreateRequest.class::cast)
        .findFirst();
  }

  private Optional<UUID> firstUuid(Object[] args) {
    return Arrays.stream(args)
        .filter(UUID.class::isInstance)
        .map(UUID.class::cast)
        .findFirst();
  }

  private String targetType(ChangeRequestCreateRequest request) {
    return request.targetType().toUpperCase(Locale.ROOT);
  }
}
