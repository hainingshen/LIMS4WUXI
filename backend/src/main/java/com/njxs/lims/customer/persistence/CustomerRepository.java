package com.njxs.lims.customer.persistence;

import com.njxs.lims.customer.domain.CustomerContact;
import com.njxs.lims.customer.domain.CustomerOrg;
import com.njxs.lims.customer.domain.Manufacturer;

public interface CustomerRepository {

  void saveCustomerOrg(CustomerOrg customerOrg);

  void saveCustomerContact(CustomerContact customerContact);

  void saveManufacturer(Manufacturer manufacturer);
}
