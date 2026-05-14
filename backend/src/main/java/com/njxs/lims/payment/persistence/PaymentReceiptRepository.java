package com.njxs.lims.payment.persistence;

import com.njxs.lims.payment.domain.PaymentReceiptRecord;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface PaymentReceiptRepository {

  String nextBizNo(int year);

  PaymentReceiptRecord save(PaymentReceiptRecord record, UUID actorUserId);

  List<PaymentReceiptRecord> list(UUID projectId, int limit, int offset);

  int count(UUID projectId);

  BigDecimal recordedAmountForReport(UUID reportId);

  boolean projectExists(UUID projectId);

  boolean reportBelongsToProject(UUID reportId, UUID projectId);

  void markProjectPaid(UUID projectId, UUID actorUserId);

  void unlockReportsByProject(UUID projectId, UUID actorUserId);

  void unlockReport(UUID reportId, UUID actorUserId);
}
