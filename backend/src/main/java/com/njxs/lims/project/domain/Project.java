package com.njxs.lims.project.domain;

import com.njxs.lims.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "project")
public class Project extends BaseEntity {

  @Column(name = "submission_id", nullable = false)
  private UUID submissionId;

  @Column(name = "customer_org_id", nullable = false)
  private UUID customerOrgId;

  @Column(name = "project_type", nullable = false)
  private String projectType;

  @Column(name = "qualification_type", nullable = false)
  private String qualificationType;

  @Column(name = "service_type", nullable = false)
  private String serviceType;

  @Column(name = "urgent_extra_fee_percent")
  private BigDecimal urgentExtraFeePercent;

  @Column(name = "urgent_tat_days")
  private Integer urgentTatDays;

  @Column(name = "requires_glp", nullable = false)
  private boolean requiresGlp;

  @Column(name = "requires_animal_test", nullable = false)
  private boolean requiresAnimalTest;

  @Column(name = "requires_ethics_review", nullable = false)
  private boolean requiresEthicsReview;

  @Column(name = "requires_nonstandard_review", nullable = false)
  private boolean requiresNonstandardReview;

  @Column(name = "business_owner_id", nullable = false)
  private UUID businessOwnerId;

  @Column(name = "technical_owner_id", nullable = false)
  private UUID technicalOwnerId;

  @Column(name = "sd_user_id", nullable = false)
  private UUID sdUserId;

  @Column(name = "status", nullable = false)
  private String status;

  @Column(name = "planned_finish_at")
  private Instant plannedFinishAt;

  @Column(name = "actual_finish_at")
  private Instant actualFinishAt;

  @Column(name = "health_status", nullable = false)
  private String healthStatus;

  @Column(name = "health_score", nullable = false)
  private int healthScore;

  @Column(name = "payment_status", nullable = false)
  private String paymentStatus;

  public UUID getSubmissionId() {
    return submissionId;
  }

  public void setSubmissionId(UUID submissionId) {
    this.submissionId = submissionId;
  }

  public UUID getCustomerOrgId() {
    return customerOrgId;
  }

  public void setCustomerOrgId(UUID customerOrgId) {
    this.customerOrgId = customerOrgId;
  }

  public String getProjectType() {
    return projectType;
  }

  public void setProjectType(String projectType) {
    this.projectType = projectType;
  }

  public String getQualificationType() {
    return qualificationType;
  }

  public void setQualificationType(String qualificationType) {
    this.qualificationType = qualificationType;
  }

  public String getServiceType() {
    return serviceType;
  }

  public void setServiceType(String serviceType) {
    this.serviceType = serviceType;
  }

  public BigDecimal getUrgentExtraFeePercent() {
    return urgentExtraFeePercent;
  }

  public void setUrgentExtraFeePercent(BigDecimal urgentExtraFeePercent) {
    this.urgentExtraFeePercent = urgentExtraFeePercent;
  }

  public Integer getUrgentTatDays() {
    return urgentTatDays;
  }

  public void setUrgentTatDays(Integer urgentTatDays) {
    this.urgentTatDays = urgentTatDays;
  }

  public boolean isRequiresGlp() {
    return requiresGlp;
  }

  public void setRequiresGlp(boolean requiresGlp) {
    this.requiresGlp = requiresGlp;
  }

  public boolean isRequiresAnimalTest() {
    return requiresAnimalTest;
  }

  public void setRequiresAnimalTest(boolean requiresAnimalTest) {
    this.requiresAnimalTest = requiresAnimalTest;
  }

  public boolean isRequiresEthicsReview() {
    return requiresEthicsReview;
  }

  public void setRequiresEthicsReview(boolean requiresEthicsReview) {
    this.requiresEthicsReview = requiresEthicsReview;
  }

  public boolean isRequiresNonstandardReview() {
    return requiresNonstandardReview;
  }

  public void setRequiresNonstandardReview(boolean requiresNonstandardReview) {
    this.requiresNonstandardReview = requiresNonstandardReview;
  }

  public UUID getBusinessOwnerId() {
    return businessOwnerId;
  }

  public void setBusinessOwnerId(UUID businessOwnerId) {
    this.businessOwnerId = businessOwnerId;
  }

  public UUID getTechnicalOwnerId() {
    return technicalOwnerId;
  }

  public void setTechnicalOwnerId(UUID technicalOwnerId) {
    this.technicalOwnerId = technicalOwnerId;
  }

  public UUID getSdUserId() {
    return sdUserId;
  }

  public void setSdUserId(UUID sdUserId) {
    this.sdUserId = sdUserId;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Instant getPlannedFinishAt() {
    return plannedFinishAt;
  }

  public void setPlannedFinishAt(Instant plannedFinishAt) {
    this.plannedFinishAt = plannedFinishAt;
  }

  public Instant getActualFinishAt() {
    return actualFinishAt;
  }

  public void setActualFinishAt(Instant actualFinishAt) {
    this.actualFinishAt = actualFinishAt;
  }

  public String getHealthStatus() {
    return healthStatus;
  }

  public void setHealthStatus(String healthStatus) {
    this.healthStatus = healthStatus;
  }

  public int getHealthScore() {
    return healthScore;
  }

  public void setHealthScore(int healthScore) {
    this.healthScore = healthScore;
  }

  public String getPaymentStatus() {
    return paymentStatus;
  }

  public void setPaymentStatus(String paymentStatus) {
    this.paymentStatus = paymentStatus;
  }
}
