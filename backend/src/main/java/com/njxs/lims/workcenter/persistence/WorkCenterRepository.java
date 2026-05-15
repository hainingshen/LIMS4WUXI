package com.njxs.lims.workcenter.persistence;

import com.njxs.lims.workcenter.domain.WorkCenterProjectRecord;
import com.njxs.lims.workcenter.domain.WorkCenterResourceLoadRecord;
import com.njxs.lims.workcenter.domain.WorkCenterScope;
import com.njxs.lims.workcenter.domain.WorkCenterSummaryRecord;
import com.njxs.lims.workcenter.domain.WorkCenterTaskRecord;
import com.njxs.lims.workcenter.domain.WorkCenterTrendRecord;
import java.util.List;

public interface WorkCenterRepository {

  WorkCenterSummaryRecord summary(WorkCenterScope scope);

  List<WorkCenterProjectRecord> projects(WorkCenterScope scope, String filter, int limit, int offset);

  int countProjects(WorkCenterScope scope, String filter);

  List<WorkCenterResourceLoadRecord> resourceLoad(WorkCenterScope scope);

  List<WorkCenterTrendRecord> trends(WorkCenterScope scope);

  List<WorkCenterTaskRecord> tasks(WorkCenterScope scope, int limit);
}
