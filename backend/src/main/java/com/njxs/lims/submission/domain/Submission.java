package com.njxs.lims.submission.domain;

import com.njxs.lims.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "submission")
public class Submission extends BaseEntity {

  @Column(name = "customer_org_id", nullable = false)
  private UUID customerOrgId;

  @Column(name = "customer_contact_id", nullable = false)
  private UUID customerContactId;

  @Column(name = "manufacturer_id", nullable = false)
  private UUID manufacturerId;

  @Column(name = "applicant_user_id", nullable = false)
  private UUID applicantUserId;

  @Column(name = "assigned_sales_user_id")
  private UUID assignedSalesUserId;

  @Column(name = "form_code", nullable = false)
  private String formCode;

  @Column(name = "schema_version", nullable = false)
  private Integer schemaVersion;

  @Column(name = "data_json", nullable = false)
  private String dataJson;

  @Column(name = "status", nullable = false)
  private String status;

  @Column(name = "official_report_email", nullable = false)
  private String officialReportEmail;

  @Column(name = "shipping_info", nullable = false)
  private String shippingInfo;

  @Column(name = "requires_glp", nullable = false)
  private boolean requiresGlp;

  @Column(name = "requires_hard_copy_of_electronic_report", nullable = false)
  private boolean requiresHardCopyOfElectronicReport;

  @Column(name = "report_language", nullable = false)
  private String reportLanguage;

  @Column(name = "protocol_language", nullable = false)
  private String protocolLanguage;

  @Column(name = "submitted_quantity_text", nullable = false)
  private String submittedQuantityText;

  @Column(name = "sample_disposition", nullable = false)
  private String sampleDisposition;

  @Column(name = "sample_disposition_remark")
  private String sampleDispositionRemark;

  @Column(name = "change_locked_at")
  private Instant changeLockedAt;

  @Column(name = "glp_characterization_affirmed")
  private Boolean glpCharacterizationAffirmed;

  @Column(name = "glp_characterization_statement_version")
  private String glpCharacterizationStatementVersion;

  @Column(name = "glp_characterization_confirmed_by")
  private String glpCharacterizationConfirmedBy;

  @Column(name = "glp_characterization_confirmed_at")
  private Instant glpCharacterizationConfirmedAt;

  public UUID getCustomerOrgId() {
    return customerOrgId;
  }

  public void setCustomerOrgId(UUID customerOrgId) {
    this.customerOrgId = customerOrgId;
  }

  public UUID getCustomerContactId() {
    return customerContactId;
  }

  public void setCustomerContactId(UUID customerContactId) {
    this.customerContactId = customerContactId;
  }

  public UUID getManufacturerId() {
    return manufacturerId;
  }

  public void setManufacturerId(UUID manufacturerId) {
    this.manufacturerId = manufacturerId;
  }

  public UUID getApplicantUserId() {
    return applicantUserId;
  }

  public void setApplicantUserId(UUID applicantUserId) {
    this.applicantUserId = applicantUserId;
  }

  public UUID getAssignedSalesUserId() {
    return assignedSalesUserId;
  }

  public void setAssignedSalesUserId(UUID assignedSalesUserId) {
    this.assignedSalesUserId = assignedSalesUserId;
  }

  public String getFormCode() {
    return formCode;
  }

  public void setFormCode(String formCode) {
    this.formCode = formCode;
  }

  public Integer getSchemaVersion() {
    return schemaVersion;
  }

  public void setSchemaVersion(Integer schemaVersion) {
    this.schemaVersion = schemaVersion;
  }

  public String getDataJson() {
    return dataJson;
  }

  public void setDataJson(String dataJson) {
    this.dataJson = dataJson;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getOfficialReportEmail() {
    return officialReportEmail;
  }

  public void setOfficialReportEmail(String officialReportEmail) {
    this.officialReportEmail = officialReportEmail;
  }

  public String getShippingInfo() {
    return shippingInfo;
  }

  public void setShippingInfo(String shippingInfo) {
    this.shippingInfo = shippingInfo;
  }

  public boolean isRequiresGlp() {
    return requiresGlp;
  }

  public void setRequiresGlp(boolean requiresGlp) {
    this.requiresGlp = requiresGlp;
  }

  public boolean isRequiresHardCopyOfElectronicReport() {
    return requiresHardCopyOfElectronicReport;
  }

  public void setRequiresHardCopyOfElectronicReport(boolean requiresHardCopyOfElectronicReport) {
    this.requiresHardCopyOfElectronicReport = requiresHardCopyOfElectronicReport;
  }

  public String getReportLanguage() {
    return reportLanguage;
  }

  public void setReportLanguage(String reportLanguage) {
    this.reportLanguage = reportLanguage;
  }

  public String getProtocolLanguage() {
    return protocolLanguage;
  }

  public void setProtocolLanguage(String protocolLanguage) {
    this.protocolLanguage = protocolLanguage;
  }

  public String getSubmittedQuantityText() {
    return submittedQuantityText;
  }

  public void setSubmittedQuantityText(String submittedQuantityText) {
    this.submittedQuantityText = submittedQuantityText;
  }

  public String getSampleDisposition() {
    return sampleDisposition;
  }

  public void setSampleDisposition(String sampleDisposition) {
    this.sampleDisposition = sampleDisposition;
  }

  public String getSampleDispositionRemark() {
    return sampleDispositionRemark;
  }

  public void setSampleDispositionRemark(String sampleDispositionRemark) {
    this.sampleDispositionRemark = sampleDispositionRemark;
  }

  public Instant getChangeLockedAt() {
    return changeLockedAt;
  }

  public void setChangeLockedAt(Instant changeLockedAt) {
    this.changeLockedAt = changeLockedAt;
  }

  public Boolean getGlpCharacterizationAffirmed() {
    return glpCharacterizationAffirmed;
  }

  public void setGlpCharacterizationAffirmed(Boolean glpCharacterizationAffirmed) {
    this.glpCharacterizationAffirmed = glpCharacterizationAffirmed;
  }

  public String getGlpCharacterizationStatementVersion() {
    return glpCharacterizationStatementVersion;
  }

  public void setGlpCharacterizationStatementVersion(String glpCharacterizationStatementVersion) {
    this.glpCharacterizationStatementVersion = glpCharacterizationStatementVersion;
  }

  public String getGlpCharacterizationConfirmedBy() {
    return glpCharacterizationConfirmedBy;
  }

  public void setGlpCharacterizationConfirmedBy(String glpCharacterizationConfirmedBy) {
    this.glpCharacterizationConfirmedBy = glpCharacterizationConfirmedBy;
  }

  public Instant getGlpCharacterizationConfirmedAt() {
    return glpCharacterizationConfirmedAt;
  }

  public void setGlpCharacterizationConfirmedAt(Instant glpCharacterizationConfirmedAt) {
    this.glpCharacterizationConfirmedAt = glpCharacterizationConfirmedAt;
  }
}
