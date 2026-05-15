package com.njxs.lims.quotation.api;

import com.njxs.lims.auth.domain.ClientType;
import com.njxs.lims.common.api.PageResponse;
import com.njxs.lims.permission.RequiresClient;
import com.njxs.lims.quotation.persistence.QuotationRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/quotations")
@ConditionalOnProperty(prefix = "lims.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class QuotationController {

  private final QuotationRepository quotationRepository;

  public QuotationController(QuotationRepository quotationRepository) {
    this.quotationRepository = quotationRepository;
  }

  @GetMapping
  @RequiresClient(ClientType.INTERNAL)
  public PageResponse<QuotationResponse> list(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int pageSize
  ) {
    int normalizedPage = Math.max(page, 1);
    int normalizedPageSize = Math.min(Math.max(pageSize, 1), 100);
    int offset = (normalizedPage - 1) * normalizedPageSize;
    return new PageResponse<>(
        quotationRepository.list(normalizedPageSize, offset).stream().map(QuotationResponse::from).toList(),
        normalizedPage,
        normalizedPageSize,
        quotationRepository.count()
    );
  }
}
