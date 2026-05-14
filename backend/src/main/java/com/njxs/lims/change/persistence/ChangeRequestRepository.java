package com.njxs.lims.change.persistence;

import com.njxs.lims.change.domain.ChangeRequestRecord;
import java.util.List;
import java.util.UUID;

public interface ChangeRequestRepository {

  String nextBizNo(int year);

  ChangeRequestRecord save(ChangeRequestRecord record);

  List<ChangeRequestRecord> list(int limit, int offset);

  int count();

  boolean issuedReportExistsForSample(UUID sampleId);
}
