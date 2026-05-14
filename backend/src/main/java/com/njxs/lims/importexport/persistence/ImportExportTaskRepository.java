package com.njxs.lims.importexport.persistence;

import com.njxs.lims.importexport.domain.ImportExportTaskRecord;
import java.util.UUID;

public interface ImportExportTaskRepository {

  String nextBizNo(int year);

  ImportExportTaskRecord save(ImportExportTaskRecord record, String parametersJson, UUID actorUserId);
}
