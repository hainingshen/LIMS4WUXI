package com.njxs.lims.audit;

public interface AuditLogRepository {

  void append(AuditLogEntry entry);
}
