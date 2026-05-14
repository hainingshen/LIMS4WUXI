package com.njxs.lims.customer.domain;

import com.njxs.lims.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "customer_org")
public class CustomerOrg extends BaseEntity {

  @Column(name = "name_zh", nullable = false)
  private String nameZh;

  @Column(name = "name_en")
  private String nameEn;

  @Column(name = "address_zh", nullable = false)
  private String addressZh;

  @Column(name = "address_en")
  private String addressEn;

  @Column(name = "industry")
  private String industry;

  @Column(name = "grade")
  private String grade;

  @Column(name = "tax_no")
  private String taxNo;

  @Column(name = "invoice_title")
  private String invoiceTitle;

  @Column(name = "bank_account")
  private String bankAccount;

  @Column(name = "status", nullable = false)
  private String status;

  public String getNameZh() {
    return nameZh;
  }

  public void setNameZh(String nameZh) {
    this.nameZh = nameZh;
  }

  public String getNameEn() {
    return nameEn;
  }

  public void setNameEn(String nameEn) {
    this.nameEn = nameEn;
  }

  public String getAddressZh() {
    return addressZh;
  }

  public void setAddressZh(String addressZh) {
    this.addressZh = addressZh;
  }

  public String getAddressEn() {
    return addressEn;
  }

  public void setAddressEn(String addressEn) {
    this.addressEn = addressEn;
  }

  public String getIndustry() {
    return industry;
  }

  public void setIndustry(String industry) {
    this.industry = industry;
  }

  public String getGrade() {
    return grade;
  }

  public void setGrade(String grade) {
    this.grade = grade;
  }

  public String getTaxNo() {
    return taxNo;
  }

  public void setTaxNo(String taxNo) {
    this.taxNo = taxNo;
  }

  public String getInvoiceTitle() {
    return invoiceTitle;
  }

  public void setInvoiceTitle(String invoiceTitle) {
    this.invoiceTitle = invoiceTitle;
  }

  public String getBankAccount() {
    return bankAccount;
  }

  public void setBankAccount(String bankAccount) {
    this.bankAccount = bankAccount;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }
}
