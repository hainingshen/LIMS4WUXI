package com.njxs.lims.greenchannel.service;

import com.njxs.lims.approval.domain.ApprovalOutcome;
import com.njxs.lims.approval.service.ApprovalRecordService;
import com.njxs.lims.auth.domain.AuthPrincipal;
import com.njxs.lims.common.api.PageResponse;
import com.njxs.lims.common.exception.BusinessException;
import com.njxs.lims.greenchannel.api.GreenChannelApprovalRequest;
import com.njxs.lims.greenchannel.api.GreenChannelCreateRequest;
import com.njxs.lims.greenchannel.api.GreenChannelResponse;
import com.njxs.lims.greenchannel.domain.GreenChannelRequestRecord;
import com.njxs.lims.greenchannel.persistence.GreenChannelRepository;
import com.njxs.lims.payment.service.PaymentGateStatus;
import com.njxs.lims.permission.PermissionService;
import com.njxs.lims.report.delivery.service.ReportDeliveryLogService;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(prefix = "lims.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class GreenChannelService {

  private final GreenChannelRepository greenChannelRepository;
  private final ApprovalRecordService approvalRecordService;
  private final PermissionService permissionService;
  private final ReportDeliveryLogService reportDeliveryLogService;

  public GreenChannelService(
      GreenChannelRepository greenChannelRepository,
      ApprovalRecordService approvalRecordService,
      PermissionService permissionService,
      ReportDeliveryLogService reportDeliveryLogService
  ) {
    this.greenChannelRepository = greenChannelRepository;
    this.approvalRecordService = approvalRecordService;
    this.permissionService = permissionService;
    this.reportDeliveryLogService = reportDeliveryLogService;
  }

  public PageResponse<GreenChannelResponse> list(UUID projectId, int page, int pageSize) {
    int normalizedPage = Math.max(page, 1);
    int normalizedPageSize = Math.min(Math.max(pageSize, 1), 100);
    int offset = (normalizedPage - 1) * normalizedPageSize;
    return new PageResponse<>(
        greenChannelRepository.list(projectId, normalizedPageSize, offset).stream()
            .map(GreenChannelResponse::from)
            .toList(),
        normalizedPage,
        normalizedPageSize,
        greenChannelRepository.count(projectId)
    );
  }

  @Transactional
  public GreenChannelResponse create(GreenChannelCreateRequest request, AuthPrincipal principal) {
    if (!greenChannelRepository.reportBelongsToProject(request.reportId(), request.projectId())) {
      throw new BusinessException(
          "GREEN_CHANNEL_REPORT_SCOPE_INVALID",
          "绿色通道报告不属于指定项目",
          HttpStatus.CONFLICT,
          Map.of("projectId", request.projectId(), "reportId", request.reportId())
      );
    }
    Instant now = Instant.now();
    GreenChannelRequestRecord record = new GreenChannelRequestRecord(
        UUID.randomUUID(),
        greenChannelRepository.nextBizNo(now.atZone(ZoneOffset.UTC).getYear()),
        request.projectId(),
        request.reportId(),
        requireText("reason", request.reason()),
        "DEPT_REVIEWING",
        principal.userId(),
        now,
        "ELECTRONIC_REPORT_ONLY"
    );
    return GreenChannelResponse.from(greenChannelRepository.save(record, principal.userId()));
  }

  @Transactional
  public void approve(UUID id, GreenChannelApprovalRequest request, AuthPrincipal principal) {
    String stage = requireText("stage", normalize(request.stage()));
    String actionCode = actionCode(stage);
    permissionService.assertAllowed(principal, actionCode, new Object[] {id, request});
    ApprovalOutcome outcome = ApprovalOutcome.require(normalize(request.outcome()));
    if (outcome == ApprovalOutcome.NA) {
      throw new BusinessException(
          "GREEN_CHANNEL_OUTCOME_INVALID",
          "绿色通道审批结论不合法",
          HttpStatus.BAD_REQUEST,
          Map.of("outcome", request.outcome())
      );
    }
    GreenChannelRequestRecord record = greenChannel(id);
    String nextStatus = nextStatus(record, stage, outcome);
    approvalRecordService.record(
        "green_channel_request",
        record.id(),
        record.bizNo(),
        stage,
        outcome.name(),
        request.comment(),
        principal,
        null
    );
    greenChannelRepository.updateStatus(id, nextStatus, principal.userId());
    if ("APPROVED".equals(nextStatus)) {
      greenChannelRepository.greenUnlockReport(record.reportId(), principal.userId());
      reportDeliveryLogService.recordForCurrentVersion(
          record.reportId(),
          "GREEN_CHANNEL_RELEASE",
          principal,
          PaymentGateStatus.GREEN_UNLOCKED,
          null
      );
    }
  }

  private String nextStatus(GreenChannelRequestRecord record, String stage, ApprovalOutcome outcome) {
    return switch (stage) {
      case "DEPT" -> {
        requireStatus(record, "DEPT_REVIEWING");
        yield outcome == ApprovalOutcome.APPROVED ? "MANAGEMENT_REVIEWING" : "REJECTED";
      }
      case "MANAGEMENT" -> {
        requireStatus(record, "MANAGEMENT_REVIEWING");
        yield outcome == ApprovalOutcome.APPROVED ? "APPROVED" : "REJECTED";
      }
      default -> throw new BusinessException(
          "GREEN_CHANNEL_STAGE_INVALID",
          "绿色通道审批阶段不合法",
          HttpStatus.BAD_REQUEST,
          Map.of("stage", stage)
      );
    };
  }

  private void requireStatus(GreenChannelRequestRecord record, String expected) {
    if (!expected.equals(record.status())) {
      throw new BusinessException(
          "GREEN_CHANNEL_STATUS_CONFLICT",
          "绿色通道当前状态不可执行该操作",
          HttpStatus.CONFLICT,
          Map.of("expected", expected, "actual", record.status())
      );
    }
  }

  private GreenChannelRequestRecord greenChannel(UUID id) {
    return greenChannelRepository.findById(id)
        .orElseThrow(() -> new BusinessException(
            "NOT_FOUND",
            "资源不存在",
            HttpStatus.NOT_FOUND,
            Map.of("entityType", "green_channel_request", "entityId", id)
        ));
  }

  private String actionCode(String stage) {
    return switch (stage) {
      case "DEPT" -> "GREEN_CHANNEL_DEPT_APPROVE";
      case "MANAGEMENT" -> "GREEN_CHANNEL_FINAL_APPROVE";
      default -> throw new BusinessException(
          "GREEN_CHANNEL_STAGE_INVALID",
          "绿色通道审批阶段不合法",
          HttpStatus.BAD_REQUEST,
          Map.of("stage", stage)
      );
    };
  }

  private String normalize(String value) {
    return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
  }

  private String requireText(String field, String value) {
    if (value == null || value.isBlank()) {
      throw new BusinessException(
          "VALIDATION_FAILED",
          "请求参数校验失败",
          HttpStatus.BAD_REQUEST,
          Map.of(field, "required")
      );
    }
    return value.trim();
  }
}
