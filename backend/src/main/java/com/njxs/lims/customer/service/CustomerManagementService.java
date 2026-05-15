package com.njxs.lims.customer.service;

import com.njxs.lims.auth.domain.AuthPrincipal;
import com.njxs.lims.common.api.PageResponse;
import com.njxs.lims.common.exception.BusinessException;
import com.njxs.lims.customer.api.CustomerOrgResponse;
import com.njxs.lims.customer.api.CustomerOrgUpsertRequest;
import com.njxs.lims.customer.api.PortalUserCreateRequest;
import com.njxs.lims.customer.api.PortalUserResponse;
import com.njxs.lims.customer.domain.CustomerOrgRecord;
import com.njxs.lims.customer.domain.PortalUserRecord;
import com.njxs.lims.customer.persistence.CustomerRepository;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(prefix = "lims.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CustomerManagementService {

  private static final Set<String> PORTAL_ROLES = Set.of(
      "CUSTOMER_ORG_ADMIN",
      "CUSTOMER_APPLICANT",
      "CUSTOMER_VIEWER"
  );
  private static final String DEFAULT_PORTAL_PASSWORD = "Applicant123!";

  private final CustomerRepository customerRepository;
  private final PasswordEncoder passwordEncoder;

  public CustomerManagementService(CustomerRepository customerRepository, PasswordEncoder passwordEncoder) {
    this.customerRepository = customerRepository;
    this.passwordEncoder = passwordEncoder;
  }

  public PageResponse<CustomerOrgResponse> listCustomerOrgs(String keyword, int page, int pageSize) {
    int normalizedPage = Math.max(page, 1);
    int normalizedPageSize = Math.min(Math.max(pageSize, 1), 100);
    int offset = (normalizedPage - 1) * normalizedPageSize;
    String normalizedKeyword = normalizeOptionalText(keyword);
    return new PageResponse<>(
        customerRepository.listCustomerOrgs(normalizedKeyword, normalizedPageSize, offset).stream()
            .map(CustomerOrgResponse::from)
            .toList(),
        normalizedPage,
        normalizedPageSize,
        customerRepository.countCustomerOrgs(normalizedKeyword)
    );
  }

  @Transactional
  public CustomerOrgResponse upsertCustomerOrg(CustomerOrgUpsertRequest request, AuthPrincipal principal) {
    UUID id = request.id() == null ? UUID.randomUUID() : request.id();
    if (request.id() != null && !customerRepository.existsCustomerOrg(request.id())) {
      throw notFound("customer_org", request.id());
    }
    CustomerOrgRecord record = new CustomerOrgRecord(
        id,
        requireText("nameZh", request.nameZh()),
        normalizeOptionalText(request.nameEn()),
        requireText("addressZh", request.addressZh()),
        normalizeOptionalText(request.addressEn()),
        normalizeOptionalText(request.taxNo()),
        normalizeOptionalText(request.invoiceTitle()),
        request.id() == null ? "ACTIVE" : customerRepository.customerOrgStatus(request.id()).orElse("ACTIVE")
    );
    CustomerOrgRecord saved = request.id() == null
        ? customerRepository.insertCustomerOrg(
            record,
            customerRepository.nextCustomerOrgBizNo(Instant.now().atZone(ZoneOffset.UTC).getYear()),
            principal.userId()
        )
        : customerRepository.updateCustomerOrg(record, principal.userId());
    return CustomerOrgResponse.from(saved);
  }

  public PageResponse<PortalUserResponse> listPortalUsers(UUID customerOrgId, int page, int pageSize) {
    int normalizedPage = Math.max(page, 1);
    int normalizedPageSize = Math.min(Math.max(pageSize, 1), 100);
    int offset = (normalizedPage - 1) * normalizedPageSize;
    return new PageResponse<>(
        customerRepository.listPortalUsers(customerOrgId, normalizedPageSize, offset).stream()
            .map(PortalUserResponse::from)
            .toList(),
        normalizedPage,
        normalizedPageSize,
        customerRepository.countPortalUsers(customerOrgId)
    );
  }

  @Transactional
  public PortalUserResponse createPortalUser(PortalUserCreateRequest request, AuthPrincipal principal) {
    if (!customerRepository.existsCustomerOrg(request.customerOrgId())) {
      throw notFound("customer_org", request.customerOrgId());
    }
    String username = requireText("username", request.username());
    if (customerRepository.existsUsername(username)) {
      throw new BusinessException(
          "PORTAL_USER_USERNAME_CONFLICT",
          "门户账号用户名已存在",
          HttpStatus.CONFLICT,
          Map.of("username", username)
      );
    }
    List<String> roleCodes = normalizePortalRoles(request.roleCodes());
    PortalUserRecord saved = customerRepository.insertPortalUser(
        new PortalUserRecord(
            UUID.randomUUID(),
            request.customerOrgId(),
            username,
            requireText("displayName", request.displayName()),
            normalizeOptionalText(request.email()),
            roleCodes,
            "ACTIVE"
        ),
        passwordEncoder.encode(DEFAULT_PORTAL_PASSWORD),
        principal.userId()
    );
    return PortalUserResponse.from(saved);
  }

  private List<String> normalizePortalRoles(List<String> roleCodes) {
    if (roleCodes == null || roleCodes.isEmpty()) {
      throw validation("roleCodes", "required");
    }
    LinkedHashSet<String> normalized = new LinkedHashSet<>();
    for (String roleCode : roleCodes) {
      String value = requireText("roleCodes", roleCode).toUpperCase(Locale.ROOT);
      if (!PORTAL_ROLES.contains(value)) {
        throw new BusinessException(
            "PORTAL_USER_ROLE_INVALID",
            "门户账号角色不合法",
            HttpStatus.BAD_REQUEST,
            Map.of("roleCode", roleCode)
        );
      }
      normalized.add(value);
    }
    return List.copyOf(normalized);
  }

  private String requireText(String field, String value) {
    String normalized = normalizeOptionalText(value);
    if (normalized == null) {
      throw validation(field, "required");
    }
    return normalized;
  }

  private String normalizeOptionalText(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }

  private BusinessException validation(String field, String reason) {
    return new BusinessException(
        "VALIDATION_FAILED",
        "请求参数校验失败",
        HttpStatus.BAD_REQUEST,
        Map.of(field, reason)
    );
  }

  private BusinessException notFound(String entityType, UUID entityId) {
    return new BusinessException(
        "NOT_FOUND",
        "资源不存在",
        HttpStatus.NOT_FOUND,
        Map.of("entityType", entityType, "entityId", entityId)
    );
  }
}
