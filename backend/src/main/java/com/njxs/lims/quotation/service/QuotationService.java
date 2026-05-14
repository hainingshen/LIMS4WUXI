package com.njxs.lims.quotation.service;

import com.njxs.lims.auth.domain.AuthPrincipal;
import com.njxs.lims.common.exception.BusinessException;
import com.njxs.lims.project.domain.Project;
import com.njxs.lims.project.domain.ProjectStatus;
import com.njxs.lims.project.persistence.ProjectRepository;
import com.njxs.lims.project.service.ProjectStatusTransitionService;
import com.njxs.lims.quotation.api.QuotationCreateRequest;
import com.njxs.lims.quotation.api.QuotationResponse;
import com.njxs.lims.quotation.domain.Quotation;
import com.njxs.lims.quotation.domain.QuotationItem;
import com.njxs.lims.quotation.domain.QuotationStatus;
import com.njxs.lims.quotation.persistence.QuotationRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(prefix = "lims.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class QuotationService {

  private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

  private final ProjectRepository projectRepository;
  private final ProjectStatusTransitionService transitionService;
  private final QuotationRepository quotationRepository;

  public QuotationService(
      ProjectRepository projectRepository,
      ProjectStatusTransitionService transitionService,
      QuotationRepository quotationRepository
  ) {
    this.projectRepository = projectRepository;
    this.transitionService = transitionService;
    this.quotationRepository = quotationRepository;
  }

  @Transactional
  public QuotationResponse create(UUID projectId, QuotationCreateRequest request, AuthPrincipal principal) {
    Project project = projectRepository.findById(projectId)
        .orElseThrow(() -> new BusinessException(
            "NOT_FOUND",
            "资源不存在",
            HttpStatus.NOT_FOUND,
            Map.of("entityType", "project", "entityId", projectId)
        ));
    assertQuotable(project);

    UUID quotationId = UUID.randomUUID();
    BigDecimal discount = amount(request.discountAmount());
    List<QuotationItem> items = items(quotationId, request.items());
    BigDecimal subtotal = items.stream()
        .map(QuotationItem::lineAmount)
        .reduce(ZERO, BigDecimal::add);
    BigDecimal total = subtotal.subtract(discount).max(ZERO).setScale(2, RoundingMode.HALF_UP);

    Quotation quotation = quotation(project, request, principal, quotationId, subtotal, discount, total);
    quotationRepository.save(quotation, items);
    transitionService.transition(project, ProjectStatus.QUOTED, "quotation created");
    projectRepository.updateStatus(project, principal.userId());
    return QuotationResponse.from(quotation);
  }

  public void assertQuotable(Project project) {
    if (!ProjectStatus.QUOTABLE.name().equals(project.getStatus())) {
      throw new BusinessException(
          "PROJECT_NOT_QUOTABLE",
          "项目未达到可报价状态",
          HttpStatus.CONFLICT,
          Map.of("expected", ProjectStatus.QUOTABLE.name(), "actual", project.getStatus())
      );
    }
  }

  private Quotation quotation(
      Project project,
      QuotationCreateRequest request,
      AuthPrincipal principal,
      UUID quotationId,
      BigDecimal subtotal,
      BigDecimal discount,
      BigDecimal total
  ) {
    Instant now = Instant.now();
    Quotation quotation = new Quotation();
    quotation.setId(quotationId);
    quotation.setBizNo(quotationRepository.nextQuotationBizNo(Year.now().getValue()));
    quotation.setProjectId(project.getId());
    quotation.setCustomerOrgId(project.getCustomerOrgId());
    quotation.setStatus(QuotationStatus.DRAFT.name());
    quotation.setCurrency("CNY");
    quotation.setSubtotalAmount(subtotal);
    quotation.setDiscountAmount(discount);
    quotation.setTotalAmount(total);
    quotation.setValidUntil(LocalDate.now().plusDays(30));
    quotation.setNote(emptyToNull(request.note()));
    quotation.setCreatedAt(now);
    quotation.setCreatedBy(principal.userId());
    quotation.setUpdatedAt(now);
    quotation.setUpdatedBy(principal.userId());
    quotation.setTenantOrgId(project.getTenantOrgId());
    quotation.setSourceType("NEW");
    quotation.setDataQualityScore(100);
    return quotation;
  }

  private List<QuotationItem> items(UUID quotationId, List<QuotationCreateRequest.Item> requestItems) {
    List<QuotationItem> items = new ArrayList<>();
    for (int index = 0; index < requestItems.size(); index++) {
      QuotationCreateRequest.Item requestItem = requestItems.get(index);
      BigDecimal unitPrice = amount(requestItem.unitPrice());
      BigDecimal quantity = requestItem.quantity().setScale(2, RoundingMode.HALF_UP);
      BigDecimal lineAmount = unitPrice.multiply(quantity).setScale(2, RoundingMode.HALF_UP);
      items.add(new QuotationItem(
          UUID.randomUUID(),
          quotationId,
          requestItem.testName(),
          unitPrice,
          quantity,
          lineAmount,
          index + 1
      ));
    }
    return items;
  }

  private BigDecimal amount(BigDecimal value) {
    return (value == null ? ZERO : value).setScale(2, RoundingMode.HALF_UP);
  }

  private String emptyToNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value;
  }
}
