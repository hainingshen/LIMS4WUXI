package com.njxs.lims.payment.api;

import com.njxs.lims.audit.Audited;
import com.njxs.lims.auth.domain.ClientType;
import com.njxs.lims.auth.service.AuthContext;
import com.njxs.lims.common.api.PageResponse;
import com.njxs.lims.payment.service.PaymentReceiptService;
import com.njxs.lims.permission.RequiresAction;
import com.njxs.lims.permission.RequiresClient;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payment-receipts")
@ConditionalOnProperty(prefix = "lims.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PaymentReceiptController {

  private final PaymentReceiptService paymentReceiptService;

  public PaymentReceiptController(PaymentReceiptService paymentReceiptService) {
    this.paymentReceiptService = paymentReceiptService;
  }

  @GetMapping
  @RequiresClient(ClientType.INTERNAL)
  public PageResponse<PaymentReceiptResponse> list(
      @RequestParam(required = false) UUID projectId,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int pageSize
  ) {
    return paymentReceiptService.list(projectId, page, pageSize);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Audited(actionCode = "PAYMENT_RECEIPT_CREATE", entityType = "payment_receipt")
  @RequiresAction("PAYMENT_RECEIPT_CREATE")
  public PaymentReceiptResponse create(@Valid @RequestBody PaymentReceiptCreateRequest request) {
    return paymentReceiptService.create(request, AuthContext.require());
  }
}
