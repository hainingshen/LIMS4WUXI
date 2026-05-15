package com.njxs.lims.rule.persistence;

import com.njxs.lims.rule.domain.BusinessRule;
import java.util.List;
import java.util.Optional;

public interface BusinessRuleRepository {

  Optional<BusinessRule> findEnabledByRuleCode(String ruleCode);

  List<BusinessRule> list();

  BusinessRule upsert(BusinessRule rule);
}
