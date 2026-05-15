package com.njxs.lims.masterdata.persistence;

import com.njxs.lims.masterdata.domain.HolidayRecord;
import com.njxs.lims.masterdata.domain.InternalUserRecord;
import com.njxs.lims.masterdata.domain.LegacyStatusMappingRecord;
import com.njxs.lims.masterdata.domain.OrganizationUnitRecord;
import com.njxs.lims.masterdata.domain.RoleRecord;
import java.util.List;

public interface MasterDataAdminRepository {

  List<HolidayRecord> listHolidays();

  HolidayRecord upsertHoliday(HolidayRecord record);

  List<OrganizationUnitRecord> listOrganizationUnits();

  List<InternalUserRecord> listInternalUsers(int limit, int offset);

  int countInternalUsers();

  List<RoleRecord> listRoles();

  List<LegacyStatusMappingRecord> listLegacyStatusMappings();

  LegacyStatusMappingRecord upsertLegacyStatusMapping(LegacyStatusMappingRecord record);
}
