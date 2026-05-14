package com.njxs.lims.permission;

import java.util.List;

public interface PermissionRepository {

  List<PermissionRule> findRules(String actionCode, List<String> roleCodes);
}
