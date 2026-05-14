package com.njxs.lims.submission.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubmissionRepository {

  Optional<SubmissionRecord> findById(UUID id);

  String nextSubmissionBizNo(int year);

  Optional<UUID> findDefaultSalesUserId();

  List<SubmissionRecord> listForApplicant(UUID applicantUserId, int limit, int offset);

  int countForApplicant(UUID applicantUserId);

  List<SubmissionRecord> listForCustomerOrg(UUID customerOrgId, int limit, int offset);

  int countForCustomerOrg(UUID customerOrgId);

  List<SubmissionRecord> listSubmittedForAssignedSales(UUID salesUserId, int limit, int offset);

  int countSubmittedForAssignedSales(UUID salesUserId);

  boolean existsByApplicant(UUID id, UUID applicantUserId);

  boolean existsByCustomerOrg(UUID id, UUID customerOrgId);

  boolean existsByAssignedSales(UUID id, UUID salesUserId);

  SubmissionRecord markSubmitted(UUID id, UUID actorUserId);

  SubmissionRecord markAccepted(UUID id, UUID actorUserId);

  SubmissionRecord markProjectSplit(UUID id, UUID actorUserId);

  SubmissionRecord updateDraftData(
      UUID id,
      String formCode,
      int schemaVersion,
      String dataJson,
      String officialReportEmail,
      String shippingInfo,
      boolean requiresGlp,
      boolean requiresHardCopyOfElectronicReport,
      String reportLanguage,
      String protocolLanguage,
      String submittedQuantityText,
      String sampleDisposition,
      String sampleDispositionRemark,
      UUID actorUserId
  );

  void markChangeLocked(UUID id, UUID actorUserId);
}
