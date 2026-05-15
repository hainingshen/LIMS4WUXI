package com.njxs.lims.payment.persistence;

import com.njxs.lims.payment.domain.PaymentReceiptRecord;
import com.njxs.lims.permission.InternalDataScope;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface PaymentReceiptRepository {

  String nextBizNo(int year);

  PaymentReceiptRecord save(PaymentReceiptRecord record, UUID actorUserId);

  List<PaymentReceiptRecord> list(UUID projectId, int limit, int offset);

  int count(UUID projectId);

  List<PaymentReceiptRecord> listVisibleTo(InternalDataScope scope, UUID projectId, int limit, int offset);

  int countVisibleTo(InternalDataScope scope, UUID projectId);

  BigDecimal recordedAmountForReport(UUID reportId);

  boolean projectExists(UUID projectId);

  boolean reportBelongsToProject(UUID reportId, UUID projectId);

  List<UUID> findReportsForPaymentUnlockRelease(UUID projectId, UUID reportId);

  void markProjectPaid(UUID projectId, UUID actorUserId);

  void unlockReportsByProject(UUID projectId, UUID actorUserId);

  void unlockReport(UUID reportId, UUID actorUserId);
}
