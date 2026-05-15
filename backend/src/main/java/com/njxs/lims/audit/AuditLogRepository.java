package com.njxs.lims.audit;

import java.util.List;

public interface AuditLogRepository {

  void append(AuditLogEntry entry);

  List<AuditLogRecord> list(int limit, int offset);

  int count();
}
