package com.njxs.lims.contract.persistence;

import com.njxs.lims.contract.domain.Contract;
import java.util.Optional;
import java.util.UUID;

public interface ContractRepository {

  String nextContractBizNo(int year);

  void save(Contract contract);

  Optional<Contract> findById(UUID id);

  Contract markSigned(Contract contract, UUID actorUserId);

  boolean existsByAssignedSales(UUID contractId, UUID userId);

  boolean existsByApplicant(UUID contractId, UUID userId);

  boolean existsByCustomerOrg(UUID contractId, UUID customerOrgId);
}
