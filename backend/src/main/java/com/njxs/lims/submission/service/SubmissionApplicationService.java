package com.njxs.lims.submission.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.njxs.lims.auth.domain.AuthPrincipal;
import com.njxs.lims.common.api.PageResponse;
import com.njxs.lims.common.domain.BaseEntity;
import com.njxs.lims.common.exception.BusinessException;
import com.njxs.lims.customer.domain.CustomerContact;
import com.njxs.lims.customer.domain.CustomerOrg;
import com.njxs.lims.customer.domain.Manufacturer;
import com.njxs.lims.customer.persistence.CustomerRepository;
import com.njxs.lims.form.domain.FormSchemaRecord;
import com.njxs.lims.form.service.FormSchemaService;
import com.njxs.lims.form.service.FormSchemaValidator;
import com.njxs.lims.submission.api.SubmissionDraftRequest;
import com.njxs.lims.submission.api.SubmissionResponse;
import com.njxs.lims.submission.domain.Submission;
import com.njxs.lims.submission.persistence.SubmissionBaseRepository;
import com.njxs.lims.submission.persistence.SubmissionRecord;
import com.njxs.lims.submission.persistence.SubmissionRepository;
import java.time.Instant;
import java.time.Year;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(prefix = "lims.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SubmissionApplicationService {

  private final SubmissionRepository submissionRepository;
  private final SubmissionBaseRepository submissionBaseRepository;
  private final CustomerRepository customerRepository;
  private final FormSchemaService formSchemaService;
  private final FormSchemaValidator formSchemaValidator;
  private final ObjectMapper objectMapper;

  public SubmissionApplicationService(
      SubmissionRepository submissionRepository,
      SubmissionBaseRepository submissionBaseRepository,
      CustomerRepository customerRepository,
      FormSchemaService formSchemaService,
      FormSchemaValidator formSchemaValidator,
      ObjectMapper objectMapper
  ) {
    this.submissionRepository = submissionRepository;
    this.submissionBaseRepository = submissionBaseRepository;
    this.customerRepository = customerRepository;
    this.formSchemaService = formSchemaService;
    this.formSchemaValidator = formSchemaValidator;
    this.objectMapper = objectMapper;
  }

  @Transactional
  public SubmissionResponse createDraft(SubmissionDraftRequest request, AuthPrincipal principal) {
    FormSchemaRecord schema = formSchemaService.current(request.formCode(), request.schemaVersion());
    JsonNode data = request.data();

    UUID customerOrgId = UUID.randomUUID();
    UUID customerContactId = UUID.randomUUID();
    UUID manufacturerId = UUID.randomUUID();
    UUID submissionId = UUID.randomUUID();
    UUID assignedSalesUserId = submissionRepository.findDefaultSalesUserId().orElse(null);
    Instant now = Instant.now();
    int year = Year.now().getValue();

    customerRepository.saveCustomerOrg(customerOrg(customerOrgId, data, principal.userId(), now));
    customerRepository.saveCustomerContact(
        customerContact(customerContactId, customerOrgId, data, principal.userId(), now)
    );
    customerRepository.saveManufacturer(manufacturer(manufacturerId, data, principal.userId(), now));
    submissionBaseRepository.saveSubmission(submission(
        submissionId,
        submissionRepository.nextSubmissionBizNo(year),
        customerOrgId,
        customerContactId,
        manufacturerId,
        assignedSalesUserId,
        schema,
        data,
        principal.userId(),
        now
    ));

    return SubmissionResponse.from(submissionRepository.findById(submissionId)
        .orElseThrow(() -> new BusinessException(
            "NOT_FOUND",
            "资源不存在",
            HttpStatus.NOT_FOUND,
            Map.of("entityType", "submission", "entityId", submissionId)
        )));
  }

  public PageResponse<SubmissionResponse> list(AuthPrincipal principal, int page, int pageSize) {
    int normalizedPage = Math.max(page, 1);
    int normalizedPageSize = Math.min(Math.max(pageSize, 1), 100);
    int offset = (normalizedPage - 1) * normalizedPageSize;

    List<SubmissionRecord> records;
    int total;
    if (hasRole(principal, "SALES_REP")) {
      records = submissionRepository.listSubmittedForAssignedSales(principal.userId(), normalizedPageSize, offset);
      total = submissionRepository.countSubmittedForAssignedSales(principal.userId());
    } else if (hasRole(principal, "CUSTOMER_ORG_ADMIN") && principal.orgId() != null) {
      records = submissionRepository.listForCustomerOrg(principal.orgId(), normalizedPageSize, offset);
      total = submissionRepository.countForCustomerOrg(principal.orgId());
    } else if (hasRole(principal, "CUSTOMER_APPLICANT")) {
      records = submissionRepository.listForApplicant(principal.userId(), normalizedPageSize, offset);
      total = submissionRepository.countForApplicant(principal.userId());
    } else {
      records = List.of();
      total = 0;
    }
    return new PageResponse<>(
        records.stream().map(SubmissionResponse::from).toList(),
        normalizedPage,
        normalizedPageSize,
        total
    );
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
    FormSchemaRecord schema = formSchemaService.current(current.formCode(), current.schemaVersion());
    formSchemaValidator.validate(schema.schemaJson(), dataJson(current.dataJson()));
    return SubmissionResponse.from(submissionRepository.markSubmitted(id, principal.userId()));
  }

  @Transactional
  public SubmissionResponse update(UUID id, SubmissionDraftRequest request, AuthPrincipal principal) {
    SubmissionRecord current = submissionRepository.findById(id)
        .orElseThrow(() -> new BusinessException(
            "NOT_FOUND",
            "资源不存在",
            HttpStatus.NOT_FOUND,
            Map.of("entityType", "submission", "entityId", id)
        ));
    FormSchemaRecord schema = formSchemaService.current(request.formCode(), request.schemaVersion());
    JsonNode data = request.data();

    if (current.changeLockedAt() != null) {
      FormSchemaRecord lockedSchema = formSchemaService.current(current.formCode(), current.schemaVersion());
      assertNoChangeRequestFieldChanged(current, lockedSchema, data);
    }
    if (!"DRAFT".equals(current.status())) {
      throw new BusinessException(
          "SUBMISSION_STATUS_CONFLICT",
          "送检单当前状态不可直接修改",
          HttpStatus.CONFLICT,
          Map.of("expected", "DRAFT", "actual", current.status())
      );
    }

    return SubmissionResponse.from(submissionRepository.updateDraftData(
        id,
        schema.formCode(),
        schema.versionNo(),
        dataJson(data),
        text(data, "officialReportEmail"),
        text(data, "shippingInfo"),
        bool(data, "requiresGlp"),
        bool(data, "requiresHardCopyOfElectronicReport"),
        textOrDefault(data, "reportLanguage", "CHS"),
        textOrDefault(data, "protocolLanguage", "CHS"),
        text(data, "submittedQuantityText"),
        textOrDefault(data, "sampleDisposition", "DESTROY_DISCARD"),
        text(data, "sampleDispositionRemark"),
        principal.userId()
    ));
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

  private CustomerOrg customerOrg(UUID id, JsonNode data, UUID actorUserId, Instant now) {
    CustomerOrg org = new CustomerOrg();
    base(org, id, "CUST-" + shortId(id), actorUserId, id, now);
    org.setNameZh(text(data, "sponsorNameZh"));
    org.setNameEn(text(data, "sponsorNameEn"));
    org.setAddressZh(text(data, "sponsorAddressZh"));
    org.setAddressEn(text(data, "sponsorAddressEn"));
    org.setStatus("ACTIVE");
    return org;
  }

  private CustomerContact customerContact(UUID id, UUID customerOrgId, JsonNode data, UUID actorUserId, Instant now) {
    CustomerContact contact = new CustomerContact();
    base(contact, id, "CONTACT-" + shortId(id), actorUserId, customerOrgId, now);
    contact.setCustomerOrgId(customerOrgId);
    contact.setContactName(text(data, "contactName"));
    contact.setPhone(text(data, "contactPhone"));
    contact.setEmail(text(data, "contactEmail"));
    contact.setPrimary(true);
    contact.setStatus("ACTIVE");
    return contact;
  }

  private Manufacturer manufacturer(UUID id, JsonNode data, UUID actorUserId, Instant now) {
    Manufacturer manufacturer = new Manufacturer();
    base(manufacturer, id, "MFR-" + shortId(id), actorUserId, null, now);
    manufacturer.setNameZh(text(data, "manufacturerNameZh"));
    manufacturer.setNameEn(text(data, "manufacturerNameEn"));
    manufacturer.setAddressZh(text(data, "manufacturerAddressZh"));
    manufacturer.setAddressEn(text(data, "manufacturerAddressEn"));
    manufacturer.setStatus("ACTIVE");
    return manufacturer;
  }

  private Submission submission(
      UUID id,
      String bizNo,
      UUID customerOrgId,
      UUID customerContactId,
      UUID manufacturerId,
      UUID assignedSalesUserId,
      FormSchemaRecord schema,
      JsonNode data,
      UUID actorUserId,
      Instant now
  ) {
    Submission submission = new Submission();
    base(submission, id, bizNo, actorUserId, customerOrgId, now);
    submission.setCustomerOrgId(customerOrgId);
    submission.setCustomerContactId(customerContactId);
    submission.setManufacturerId(manufacturerId);
    submission.setApplicantUserId(actorUserId);
    submission.setAssignedSalesUserId(assignedSalesUserId);
    submission.setFormCode(schema.formCode());
    submission.setSchemaVersion(schema.versionNo());
    submission.setDataJson(dataJson(data));
    submission.setStatus("DRAFT");
    submission.setOfficialReportEmail(text(data, "officialReportEmail"));
    submission.setShippingInfo(text(data, "shippingInfo"));
    submission.setRequiresGlp(bool(data, "requiresGlp"));
    submission.setRequiresHardCopyOfElectronicReport(bool(data, "requiresHardCopyOfElectronicReport"));
    submission.setReportLanguage(textOrDefault(data, "reportLanguage", "CHS"));
    submission.setProtocolLanguage(textOrDefault(data, "protocolLanguage", "CHS"));
    submission.setSubmittedQuantityText(text(data, "submittedQuantityText"));
    submission.setSampleDisposition(textOrDefault(data, "sampleDisposition", "DESTROY_DISCARD"));
    submission.setSampleDispositionRemark(text(data, "sampleDispositionRemark"));
    return submission;
  }

  private void base(BaseEntity entity, UUID id, String bizNo, UUID actorUserId, UUID tenantOrgId, Instant now) {
    entity.setId(id);
    entity.setBizNo(bizNo);
    entity.setCreatedAt(now);
    entity.setCreatedBy(actorUserId);
    entity.setUpdatedAt(now);
    entity.setUpdatedBy(actorUserId);
    entity.setTenantOrgId(tenantOrgId);
    entity.setSourceType("NEW");
    entity.setDataQualityScore(100);
  }

  private String dataJson(JsonNode data) {
    try {
      return objectMapper.writeValueAsString(data);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Unable to serialize submission payload", exception);
    }
  }

  private JsonNode dataJson(String dataJson) {
    try {
      return objectMapper.readTree(dataJson == null || dataJson.isBlank() ? "{}" : dataJson);
    } catch (JsonProcessingException exception) {
      throw new BusinessException(
          "VALIDATION_FAILED",
          "请求参数校验失败",
          HttpStatus.BAD_REQUEST,
          Map.of("dataJson", "Invalid JSON payload")
      );
    }
  }

  private void assertNoChangeRequestFieldChanged(
      SubmissionRecord current,
      FormSchemaRecord schema,
      JsonNode newData
  ) {
    JsonNode currentData = dataJson(current.dataJson());
    JsonNode properties = schema.schemaJson().path("properties");
    Iterator<Map.Entry<String, JsonNode>> fields = properties.fields();
    while (fields.hasNext()) {
      Map.Entry<String, JsonNode> field = fields.next();
      JsonNode fieldSchema = field.getValue();
      if (!"CHANGE_REQUEST".equals(fieldSchema.path("x-lock-policy").asText())) {
        continue;
      }
      String fieldName = field.getKey();
      if (sameValue(currentData.get(fieldName), newData.get(fieldName))) {
        continue;
      }
      throw new BusinessException(
          "FIELD_CHANGE_REQUIRES_CHANGE_REQUEST",
          "字段已锁定，请创建变更申请",
          HttpStatus.CONFLICT,
          Map.of(
              "fieldPath", fieldName,
              "fieldLabel", fieldSchema.path("title").asText(fieldName),
              "lockPolicy", "CHANGE_REQUEST",
              "nextAction", "CREATE_CHANGE_REQUEST"
          )
      );
    }
  }

  private boolean sameValue(JsonNode left, JsonNode right) {
    JsonNode normalizedLeft = nullOrMissing(left) ? null : left;
    JsonNode normalizedRight = nullOrMissing(right) ? null : right;
    return Objects.equals(normalizedLeft, normalizedRight);
  }

  private boolean nullOrMissing(JsonNode value) {
    return value == null || value.isMissingNode() || value.isNull();
  }

  private String text(JsonNode data, String field) {
    return textOrDefault(data, field, "");
  }

  private String textOrDefault(JsonNode data, String field, String defaultValue) {
    JsonNode value = data.get(field);
    if (value == null || value.isNull()) {
      return defaultValue;
    }
    return value.asText(defaultValue);
  }

  private boolean bool(JsonNode data, String field) {
    JsonNode value = data.get(field);
    if (value == null || value.isNull()) {
      return false;
    }
    if (value.isBoolean()) {
      return value.asBoolean();
    }
    return "true".equalsIgnoreCase(value.asText()) || "YES".equalsIgnoreCase(value.asText());
  }

  private String shortId(UUID id) {
    return id.toString().substring(0, 8).toUpperCase();
  }

  private boolean hasRole(AuthPrincipal principal, String roleCode) {
    return principal.roles().contains(roleCode);
  }
}
