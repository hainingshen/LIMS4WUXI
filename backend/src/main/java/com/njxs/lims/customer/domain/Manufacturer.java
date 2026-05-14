package com.njxs.lims.customer.domain;

import com.njxs.lims.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "manufacturer")
public class Manufacturer extends BaseEntity {

  @Column(name = "name_zh", nullable = false)
  private String nameZh;

  @Column(name = "name_en")
  private String nameEn;

  @Column(name = "address_zh", nullable = false)
  private String addressZh;

  @Column(name = "address_en")
  private String addressEn;

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

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }
}
