package com.njxs.lims.project.api;

import com.njxs.lims.project.domain.Project;
import java.util.UUID;

public record ProjectResponse(
    UUID id,
    String bizNo,
    UUID submissionId,
    UUID customerOrgId,
    String projectType,
    String qualificationType,
    boolean requiresGlp,
    boolean requiresAnimalTest,
    boolean requiresEthicsReview,
    boolean requiresNonstandardReview,
    String status,
    String paymentStatus
) {

  public static ProjectResponse from(Project project) {
    return new ProjectResponse(
        project.getId(),
        project.getBizNo(),
        project.getSubmissionId(),
        project.getCustomerOrgId(),
        project.getProjectType(),
        project.getQualificationType(),
        project.isRequiresGlp(),
        project.isRequiresAnimalTest(),
        project.isRequiresEthicsReview(),
        project.isRequiresNonstandardReview(),
        project.getStatus(),
        project.getPaymentStatus()
    );
  }
}
