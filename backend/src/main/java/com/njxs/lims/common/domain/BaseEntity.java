package com.njxs.lims.common.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@MappedSuperclass
public abstract class BaseEntity {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "biz_no", length = 64)
  private String bizNo;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "created_by", updatable = false)
  private UUID createdBy;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Column(name = "updated_by")
  private UUID updatedBy;

  @Column(name = "deleted_at")
  private Instant deletedAt;

  @Version
  @Column(name = "version", nullable = false)
  private Integer version;

  @Column(name = "tenant_org_id")
  private UUID tenantOrgId;

  @Column(name = "legacy_id", length = 128)
  private String legacyId;

  @Column(name = "source_type", length = 32)
  private String sourceType;

  @Column(name = "migration_batch", length = 64)
  private String migrationBatch;

  @Column(name = "data_quality_score")
  private Integer dataQualityScore;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getBizNo() {
    return bizNo;
  }

  public void setBizNo(String bizNo) {
    this.bizNo = bizNo;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public UUID getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(UUID createdBy) {
    this.createdBy = createdBy;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }

  public UUID getUpdatedBy() {
    return updatedBy;
  }

  public void setUpdatedBy(UUID updatedBy) {
    this.updatedBy = updatedBy;
  }

  public Instant getDeletedAt() {
    return deletedAt;
  }

  public void setDeletedAt(Instant deletedAt) {
    this.deletedAt = deletedAt;
  }

  public Integer getVersion() {
    return version;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }

  public UUID getTenantOrgId() {
    return tenantOrgId;
  }

  public void setTenantOrgId(UUID tenantOrgId) {
    this.tenantOrgId = tenantOrgId;
  }

  public String getLegacyId() {
    return legacyId;
  }

  public void setLegacyId(String legacyId) {
    this.legacyId = legacyId;
  }

  public String getSourceType() {
    return sourceType;
  }

  public void setSourceType(String sourceType) {
    this.sourceType = sourceType;
  }

  public String getMigrationBatch() {
    return migrationBatch;
  }

  public void setMigrationBatch(String migrationBatch) {
    this.migrationBatch = migrationBatch;
  }

  public Integer getDataQualityScore() {
    return dataQualityScore;
  }

  public void setDataQualityScore(Integer dataQualityScore) {
    this.dataQualityScore = dataQualityScore;
  }
}
