package com.njxs.lims.submission.service;

import com.njxs.lims.auth.domain.AuthPrincipal;
import com.njxs.lims.common.exception.BusinessException;
import com.njxs.lims.submission.api.SubmissionResponse;
import com.njxs.lims.submission.persistence.SubmissionRecord;
import com.njxs.lims.submission.persistence.SubmissionRepository;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "lims.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SubmissionApplicationService {

  private final SubmissionRepository submissionRepository;

  public SubmissionApplicationService(SubmissionRepository submissionRepository) {
    this.submissionRepository = submissionRepository;
  }

  public SubmissionResponse submit(UUID id, AuthPrincipal principal) {
    SubmissionRecord current = submissionRepository.findById(id)
        .orElseThrow(() -> new BusinessException(
            "NOT_FOUND",
            "资源不存在",
            HttpStatus.NOT_FOUND,
            Map.of("entityType", "submission", "entityId", id)
        ));
    if (!"DRAFT".equals(current.status())) {
      throw new BusinessException(
          "SUBMISSION_STATUS_CONFLICT",
          "送检单当前状态不可提交",
          HttpStatus.CONFLICT,
          Map.of("expected", "DRAFT", "actual", current.status())
      );
    }
    return SubmissionResponse.from(submissionRepository.markSubmitted(id, principal.userId()));
  }

  public SubmissionResponse accept(UUID id, AuthPrincipal principal) {
    SubmissionRecord current = submissionRepository.findById(id)
        .orElseThrow(() -> new BusinessException(
            "NOT_FOUND",
            "资源不存在",
            HttpStatus.NOT_FOUND,
            Map.of("entityType", "submission", "entityId", id)
        ));
    if (!"SUBMITTED".equals(current.status())) {
      throw new BusinessException(
          "SUBMISSION_STATUS_CONFLICT",
          "送检单当前状态不可受理",
          HttpStatus.CONFLICT,
          Map.of("expected", "SUBMITTED", "actual", current.status())
      );
    }
    return SubmissionResponse.from(submissionRepository.markAccepted(id, principal.userId()));
  }
}
