package com.njxs.lims.masterdata.persistence;

import com.njxs.lims.masterdata.domain.SystemConfigRecord;
import java.util.List;

public interface SystemConfigRepository {

  List<SystemConfigRecord> list();

  SystemConfigRecord upsert(SystemConfigRecord record);
}
