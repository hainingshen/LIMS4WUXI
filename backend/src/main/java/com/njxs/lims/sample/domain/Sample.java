package com.njxs.lims.sample.domain;

import com.njxs.lims.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "sample")
public class Sample extends BaseEntity {

  @Column(name = "project_id", nullable = false)
  private UUID projectId;

  @Column(name = "submission_id", nullable = false)
  private UUID submissionId;

  @Column(name = "sample_name_zh", nullable = false)
  private String sampleNameZh;

  @Column(name = "sample_name_en", nullable = false)
  private String sampleNameEn;

  @Column(name = "sterilization_state", nullable = false)
  private String sterilizationState;

  @Column(name = "cas_code", nullable = false)
  private String casCode;

  @Column(name = "model_ref", nullable = false)
  private String modelRef;

  @Column(name = "size_spec", nullable = false)
  private String sizeSpec;

  @Column(name = "lot_batch", nullable = false)
  private String lotBatch;

  @Column(name = "raw_material", nullable = false)
  private String rawMaterial;

  @Column(name = "packaging_material", nullable = false)
  private String packagingMaterial;

  @Column(name = "physical_state", nullable = false)
  private String physicalState;

  @Column(name = "color", nullable = false)
  private String color;

  @Column(name = "density", nullable = false)
  private String density;

  @Column(name = "stability", nullable = false)
  private String stability;

  @Column(name = "solubility", nullable = false)
  private String solubility;

  @Column(name = "storage_condition", nullable = false)
  private String storageCondition;

  @Column(name = "intended_use", nullable = false)
  private String intendedUse;

  @Column(name = "additional_info", nullable = false)
  private String additionalInfo;

  @Column(name = "quantity", nullable = false)
  private BigDecimal quantity;

  @Column(name = "quantity_unit", nullable = false)
  private String quantityUnit;

  @Column(name = "status", nullable = false)
  private String status;

  @Column(name = "received_at")
  private Instant receivedAt;

  @Column(name = "received_by")
  private UUID receivedBy;

  @Column(name = "recipient_name")
  private String recipientName;

  @Column(name = "received_date")
  private LocalDate receivedDate;

  @Column(name = "inventory_location")
  private String inventoryLocation;

  public UUID getProjectId() {
    return projectId;
  }

  public void setProjectId(UUID projectId) {
    this.projectId = projectId;
  }

  public UUID getSubmissionId() {
    return submissionId;
  }

  public void setSubmissionId(UUID submissionId) {
    this.submissionId = submissionId;
  }

  public String getSampleNameZh() {
    return sampleNameZh;
  }

  public void setSampleNameZh(String sampleNameZh) {
    this.sampleNameZh = sampleNameZh;
  }

  public String getSampleNameEn() {
    return sampleNameEn;
  }

  public void setSampleNameEn(String sampleNameEn) {
    this.sampleNameEn = sampleNameEn;
  }

  public String getSterilizationState() {
    return sterilizationState;
  }

  public void setSterilizationState(String sterilizationState) {
    this.sterilizationState = sterilizationState;
  }

  public String getCasCode() {
    return casCode;
  }

  public void setCasCode(String casCode) {
    this.casCode = casCode;
  }

  public String getModelRef() {
    return modelRef;
  }

  public void setModelRef(String modelRef) {
    this.modelRef = modelRef;
  }

  public String getSizeSpec() {
    return sizeSpec;
  }

  public void setSizeSpec(String sizeSpec) {
    this.sizeSpec = sizeSpec;
  }

  public String getLotBatch() {
    return lotBatch;
  }

  public void setLotBatch(String lotBatch) {
    this.lotBatch = lotBatch;
  }

  public String getRawMaterial() {
    return rawMaterial;
  }

  public void setRawMaterial(String rawMaterial) {
    this.rawMaterial = rawMaterial;
  }

  public String getPackagingMaterial() {
    return packagingMaterial;
  }

  public void setPackagingMaterial(String packagingMaterial) {
    this.packagingMaterial = packagingMaterial;
  }

  public String getPhysicalState() {
    return physicalState;
  }

  public void setPhysicalState(String physicalState) {
    this.physicalState = physicalState;
  }

  public String getColor() {
    return color;
  }

  public void setColor(String color) {
    this.color = color;
  }

  public String getDensity() {
    return density;
  }

  public void setDensity(String density) {
    this.density = density;
  }

  public String getStability() {
    return stability;
  }

  public void setStability(String stability) {
    this.stability = stability;
  }

  public String getSolubility() {
    return solubility;
  }

  public void setSolubility(String solubility) {
    this.solubility = solubility;
  }

  public String getStorageCondition() {
    return storageCondition;
  }

  public void setStorageCondition(String storageCondition) {
    this.storageCondition = storageCondition;
  }

  public String getIntendedUse() {
    return intendedUse;
  }

  public void setIntendedUse(String intendedUse) {
    this.intendedUse = intendedUse;
  }

  public String getAdditionalInfo() {
    return additionalInfo;
  }

  public void setAdditionalInfo(String additionalInfo) {
    this.additionalInfo = additionalInfo;
  }

  public BigDecimal getQuantity() {
    return quantity;
  }

  public void setQuantity(BigDecimal quantity) {
    this.quantity = quantity;
  }

  public String getQuantityUnit() {
    return quantityUnit;
  }

  public void setQuantityUnit(String quantityUnit) {
    this.quantityUnit = quantityUnit;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Instant getReceivedAt() {
    return receivedAt;
  }

  public void setReceivedAt(Instant receivedAt) {
    this.receivedAt = receivedAt;
  }

  public UUID getReceivedBy() {
    return receivedBy;
  }

  public void setReceivedBy(UUID receivedBy) {
    this.receivedBy = receivedBy;
  }

  public String getRecipientName() {
    return recipientName;
  }

  public void setRecipientName(String recipientName) {
    this.recipientName = recipientName;
  }

  public LocalDate getReceivedDate() {
    return receivedDate;
  }

  public void setReceivedDate(LocalDate receivedDate) {
    this.receivedDate = receivedDate;
  }

  public String getInventoryLocation() {
    return inventoryLocation;
  }

  public void setInventoryLocation(String inventoryLocation) {
    this.inventoryLocation = inventoryLocation;
  }
}
