package com.njxs.lims.approval.service;

import com.njxs.lims.approval.domain.ApprovalOutcome;
import com.njxs.lims.approval.domain.ApprovalRecord;
import com.njxs.lims.approval.persistence.ApprovalRecordRepository;
import com.njxs.lims.auth.domain.AuthPrincipal;
import com.njxs.lims.common.exception.BusinessException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(prefix = "lims.persistence", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ApprovalRecordService {

  private final ApprovalRecordRepository approvalRecordRepository;

  public ApprovalRecordService(ApprovalRecordRepository approvalRecordRepository) {
    this.approvalRecordRepository = approvalRecordRepository;
  }

  @Transactional
  public ApprovalRecord record(
      String entityType,
      UUID entityId,
      String stage,
      String outcome,
      String comment,
      UUID approvedBy
  ) {
    return record(entityType, entityId, null, stage, outcome, comment, approvedBy, null, null);
  }

  @Transactional
  public ApprovalRecord record(
      String entityType,
      UUID entityId,
      String entityBizNo,
      String stage,
      String outcome,
      String comment,
      AuthPrincipal principal,
      String traceId
  ) {
    String approverRoleCode = principal.roles().stream().findFirst().orElse(null);
    return record(
        entityType,
        entityId,
        entityBizNo,
        stage,
        outcome,
        comment,
        principal.userId(),
        approverRoleCode,
        traceId
    );
  }

  public List<ApprovalRecord> listByEntity(String entityType, UUID entityId) {
    requireEntity(entityType, entityId);
    return approvalRecordRepository.findByEntity(entityType, entityId);
  }

  public boolean hasApproved(String entityType, UUID entityId, String stage) {
    requireEntity(entityType, entityId);
    requireText("stage", stage);
    return approvalRecordRepository.existsByEntityStageAndOutcome(
        entityType,
        entityId,
        stage,
        ApprovalOutcome.APPROVED.name()
    );
  }

  private ApprovalRecord record(
      String entityType,
      UUID entityId,
      String entityBizNo,
      String stage,
      String outcome,
      String comment,
      UUID approvedBy,
      String approverRoleCode,
      String traceId
  ) {
    requireEntity(entityType, entityId);
    requireText("stage", stage);
    if (approvedBy == null) {
      throw new BusinessException(
          "VALIDATION_FAILED",
          "请求参数校验失败",
          HttpStatus.BAD_REQUEST,
          Map.of("approvedBy", "required")
      );
    }
    ApprovalRecord record = new ApprovalRecord(
        UUID.randomUUID(),
        entityType,
        entityId,
        emptyToNull(entityBizNo),
        stage,
        ApprovalOutcome.require(outcome).name(),
        emptyToNull(comment),
        approvedBy,
        emptyToNull(approverRoleCode),
        Instant.now(),
        emptyToNull(traceId)
    );
    return approvalRecordRepository.save(record);
  }

  private void requireEntity(String entityType, UUID entityId) {
    requireText("entityType", entityType);
    if (entityId == null) {
      throw new BusinessException(
          "VALIDATION_FAILED",
          "请求参数校验失败",
          HttpStatus.BAD_REQUEST,
          Map.of("entityId", "required")
      );
    }
  }

  private void requireText(String field, String value) {
    if (value == null || value.isBlank()) {
      throw new BusinessException(
          "VALIDATION_FAILED",
          "请求参数校验失败",
          HttpStatus.BAD_REQUEST,
          Map.of(field, "required")
      );
    }
  }

  private String emptyToNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value;
  }
}
