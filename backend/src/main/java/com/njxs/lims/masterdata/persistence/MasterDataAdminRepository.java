package com.njxs.lims.masterdata.persistence;

import com.njxs.lims.masterdata.domain.HolidayRecord;
import com.njxs.lims.masterdata.domain.InternalUserRecord;
import com.njxs.lims.masterdata.domain.LegacyStatusMappingRecord;
import com.njxs.lims.masterdata.domain.OrganizationUnitRecord;
import com.njxs.lims.masterdata.domain.RoleRecord;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface MasterDataAdminRepository {

  List<HolidayRecord> listHolidays();

  HolidayRecord upsertHoliday(HolidayRecord record);

  List<OrganizationUnitRecord> listOrganizationUnits();

  List<InternalUserRecord> listInternalUsers(int limit, int offset);

  int countInternalUsers();

  boolean existsUsername(String username);

  boolean existsOrganizationUnit(UUID id);

  Optional<InternalUserRecord> findInternalUser(UUID id);

  InternalUserRecord insertInternalUser(
      UUID id,
      String username,
      String displayName,
      String email,
      UUID organizationUnitId,
      String passwordHash,
      List<String> roleCodes,
      UUID actorUserId
  );

  InternalUserRecord updateInternalUser(
      UUID id,
      String displayName,
      String email,
      UUID organizationUnitId,
      List<String> roleCodes,
      UUID actorUserId
  );

  InternalUserRecord updateInternalUserStatus(UUID id, String status, UUID actorUserId);

  InternalUserRecord replaceInternalUserRoles(UUID id, List<String> roleCodes, UUID actorUserId);

  InternalUserRecord resetInternalUserPassword(UUID id, String passwordHash, UUID actorUserId);

  List<RoleRecord> listRoles();

  Set<String> activeInternalRoleCodes(List<String> roleCodes);

  Optional<RoleRecord> findRole(String roleCode);

  RoleRecord updateRoleStatus(String roleCode, String status);

  List<LegacyStatusMappingRecord> listLegacyStatusMappings();

  LegacyStatusMappingRecord upsertLegacyStatusMapping(LegacyStatusMappingRecord record);
}
