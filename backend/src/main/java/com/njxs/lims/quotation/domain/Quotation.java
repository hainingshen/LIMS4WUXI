package com.njxs.lims.quotation.domain;

import com.njxs.lims.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "quotation")
public class Quotation extends BaseEntity {

  @Column(name = "project_id", nullable = false)
  private UUID projectId;

  @Column(name = "customer_org_id", nullable = false)
  private UUID customerOrgId;

  @Column(name = "status", nullable = false)
  private String status;

  @Column(name = "currency", nullable = false)
  private String currency;

  @Column(name = "subtotal_amount", nullable = false)
  private BigDecimal subtotalAmount;

  @Column(name = "discount_amount", nullable = false)
  private BigDecimal discountAmount;

  @Column(name = "total_amount", nullable = false)
  private BigDecimal totalAmount;

  @Column(name = "valid_until")
  private LocalDate validUntil;

  @Column(name = "note")
  private String note;

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

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getCurrency() {
    return currency;
  }

  public void setCurrency(String currency) {
    this.currency = currency;
  }

  public BigDecimal getSubtotalAmount() {
    return subtotalAmount;
  }

  public void setSubtotalAmount(BigDecimal subtotalAmount) {
    this.subtotalAmount = subtotalAmount;
  }

  public BigDecimal getDiscountAmount() {
    return discountAmount;
  }

  public void setDiscountAmount(BigDecimal discountAmount) {
    this.discountAmount = discountAmount;
  }

  public BigDecimal getTotalAmount() {
    return totalAmount;
  }

  public void setTotalAmount(BigDecimal totalAmount) {
    this.totalAmount = totalAmount;
  }

  public LocalDate getValidUntil() {
    return validUntil;
  }

  public void setValidUntil(LocalDate validUntil) {
    this.validUntil = validUntil;
  }

  public String getNote() {
    return note;
  }

  public void setNote(String note) {
    this.note = note;
  }
}
