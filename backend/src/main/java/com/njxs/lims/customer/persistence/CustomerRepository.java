package com.njxs.lims.customer.persistence;

import com.njxs.lims.customer.domain.CustomerContact;
import com.njxs.lims.customer.domain.CustomerOrg;
import com.njxs.lims.customer.domain.CustomerOrgRecord;
import com.njxs.lims.customer.domain.Manufacturer;
import com.njxs.lims.customer.domain.PortalUserRecord;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository {

  void saveCustomerOrg(CustomerOrg customerOrg);

  void saveCustomerContact(CustomerContact customerContact);

  void saveManufacturer(Manufacturer manufacturer);

  List<CustomerOrgRecord> listCustomerOrgs(String keyword, int limit, int offset);

  int countCustomerOrgs(String keyword);

  boolean existsCustomerOrg(UUID id);

  Optional<String> customerOrgStatus(UUID id);

  String nextCustomerOrgBizNo(int year);

  CustomerOrgRecord insertCustomerOrg(CustomerOrgRecord record, String bizNo, UUID actorUserId);

  CustomerOrgRecord updateCustomerOrg(CustomerOrgRecord record, UUID actorUserId);

  List<PortalUserRecord> listPortalUsers(UUID customerOrgId, int limit, int offset);

  int countPortalUsers(UUID customerOrgId);

  boolean existsUsername(String username);

  PortalUserRecord insertPortalUser(PortalUserRecord record, String passwordHash, UUID actorUserId);
}
