package com.njxs.lims.customer.domain;

import com.njxs.lims.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "customer_contact")
public class CustomerContact extends BaseEntity {

  @Column(name = "customer_org_id", nullable = false)
  private UUID customerOrgId;

  @Column(name = "contact_name", nullable = false)
  private String contactName;

  @Column(name = "phone", nullable = false)
  private String phone;

  @Column(name = "email", nullable = false)
  private String email;

  @Column(name = "is_primary", nullable = false)
  private boolean primary;

  @Column(name = "status", nullable = false)
  private String status;

  public UUID getCustomerOrgId() {
    return customerOrgId;
  }

  public void setCustomerOrgId(UUID customerOrgId) {
    this.customerOrgId = customerOrgId;
  }

  public String getContactName() {
    return contactName;
  }

  public void setContactName(String contactName) {
    this.contactName = contactName;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public boolean isPrimary() {
    return primary;
  }

  public void setPrimary(boolean primary) {
    this.primary = primary;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }
}
