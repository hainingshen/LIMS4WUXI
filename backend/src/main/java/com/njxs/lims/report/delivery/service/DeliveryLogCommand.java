package com.njxs.lims.report.delivery.service;

import com.njxs.lims.auth.domain.ClientType;
import com.njxs.lims.payment.service.PaymentGateStatus;
import java.util.UUID;

public record DeliveryLogCommand(
    UUID reportId,
    UUID reportVersionId,
    String deliveryType,
    String deliveredTo,
    UUID deliveredBy,
    ClientType clientType,
    PaymentGateStatus paymentGateStatus,
    UUID fileId
) {
}
