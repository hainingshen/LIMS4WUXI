package com.njxs.lims.contract.domain;

import com.njxs.lims.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "contract")
public class Contract extends BaseEntity {

  @Column(name = "project_id", nullable = false)
  private UUID projectId;

  @Column(name = "customer_org_id", nullable = false)
  private UUID customerOrgId;

  @Column(name = "contract_type", nullable = false)
  private String contractType;

  @Column(name = "template_code", nullable = false)
  private String templateCode;

  @Column(name = "status", nullable = false)
  private String status;

  @Column(name = "source_file_id")
  private UUID sourceFileId;

  @Column(name = "signed_at")
  private Instant signedAt;

  @Column(name = "signed_file_id")
  private UUID signedFileId;

  public UUID getProjectId() {
    return projectId;
  }

  public void setProjectId(UUID projectId) {
    this.projectId = projectId;
  }

  public UUID getCustomerOrgId() {
    return customerOrgId;
  }

  public void setCustomerOrgId(UUID customerOrgId) {
    this.customerOrgId = customerOrgId;
  }

  public String getContractType() {
    return contractType;
  }

  public void setContractType(String contractType) {
    this.contractType = contractType;
  }

  public String getTemplateCode() {
    return templateCode;
  }

  public void setTemplateCode(String templateCode) {
    this.templateCode = templateCode;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public UUID getSourceFileId() {
    return sourceFileId;
  }

  public void setSourceFileId(UUID sourceFileId) {
    this.sourceFileId = sourceFileId;
  }

  public Instant getSignedAt() {
    return signedAt;
  }

  public void setSignedAt(Instant signedAt) {
    this.signedAt = signedAt;
  }

  public UUID getSignedFileId() {
    return signedFileId;
  }

  public void setSignedFileId(UUID signedFileId) {
    this.signedFileId = signedFileId;
  }
}
