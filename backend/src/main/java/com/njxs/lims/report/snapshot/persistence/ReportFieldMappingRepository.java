package com.njxs.lims.report.snapshot.persistence;

import com.njxs.lims.report.snapshot.domain.ReportFieldMapping;
import java.util.List;

public interface ReportFieldMappingRepository {

  List<ReportFieldMapping> findByTemplateCode(String templateCode);
}
