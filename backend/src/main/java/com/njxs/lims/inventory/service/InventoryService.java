package com.njxs.lims.inventory.service;

import com.njxs.lims.auth.domain.AuthPrincipal;
import com.njxs.lims.common.api.PageResponse;
import com.njxs.lims.common.exception.BusinessException;
import com.njxs.lims.inventory.api.InventoryBatchResponse;
import com.njxs.lims.inventory.api.InventoryBatchUpsertRequest;
import com.njxs.lims.inventory.api.InventoryTransactionCreateRequest;
import com.njxs.lims.inventory.api.InventoryTransactionResponse;
import com.njxs.lims.inventory.domain.InventoryBatchRecord;
import com.njxs.lims.inventory.domain.InventoryTransactionRecord;
import com.njxs.lims.inventory.persistence.InventoryRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(prefix = "lims.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class InventoryService {

  private static final Set<String> TRANSACTION_TYPES = Set.of("IN", "OUT", "USE", "RETURN", "SCRAP", "ADJUST");
  private static final Set<String> INCREASE_TYPES = Set.of("IN", "RETURN");
  private static final Set<String> DECREASE_TYPES = Set.of("OUT", "USE", "SCRAP");

  private final InventoryRepository inventoryRepository;

  public InventoryService(InventoryRepository inventoryRepository) {
    this.inventoryRepository = inventoryRepository;
  }

  public PageResponse<InventoryBatchResponse> listBatches(String keyword, int page, int pageSize) {
    int normalizedPage = Math.max(page, 1);
    int normalizedPageSize = Math.min(Math.max(pageSize, 1), 100);
    int offset = (normalizedPage - 1) * normalizedPageSize;
    return new PageResponse<>(
        inventoryRepository.listBatches(keyword, normalizedPageSize, offset).stream()
            .map(InventoryBatchResponse::from)
            .toList(),
        normalizedPage,
        normalizedPageSize,
        inventoryRepository.countBatches(keyword)
    );
  }

  @Transactional
  public InventoryBatchResponse upsertBatch(InventoryBatchUpsertRequest request, AuthPrincipal principal) {
    BigDecimal quantity = requireNonNegative("quantity", request.quantity());
    InventoryBatchRecord existing = request.id() == null ? null : batch(request.id());
    InventoryBatchRecord record = new InventoryBatchRecord(
        existing == null ? UUID.randomUUID() : existing.id(),
        requireText("batchNo", request.batchNo()),
        requireText("name", request.name()),
        emptyToNull(request.casNo()),
        emptyToNull(request.specification()),
        emptyToNull(request.manufacturer()),
        request.receivedAt(),
        request.expiryDate(),
        quantity,
        requireText("unit", request.unit()),
        requireText("storageCondition", request.storageCondition()),
        emptyToNull(request.storageLocation()),
        existing == null ? null : existing.openedAt(),
        statusFor(quantity, request.expiryDate(), existing == null ? null : existing.openedAt())
    );
    InventoryBatchRecord saved = existing == null
        ? inventoryRepository.insertBatch(record, principal.userId())
        : inventoryRepository.updateBatch(record, principal.userId()).orElseThrow(() -> notFound("inventory_batch", record.id()));
    return InventoryBatchResponse.from(saved);
  }

  public PageResponse<InventoryTransactionResponse> listTransactions(UUID inventoryBatchId, int page, int pageSize) {
    int normalizedPage = Math.max(page, 1);
    int normalizedPageSize = Math.min(Math.max(pageSize, 1), 100);
    int offset = (normalizedPage - 1) * normalizedPageSize;
    return new PageResponse<>(
        inventoryRepository.listTransactions(inventoryBatchId, normalizedPageSize, offset).stream()
            .map(InventoryTransactionResponse::from)
            .toList(),
        normalizedPage,
        normalizedPageSize,
        inventoryRepository.countTransactions(inventoryBatchId)
    );
  }

  @Transactional
  public InventoryTransactionResponse createTransaction(
      InventoryTransactionCreateRequest request,
      AuthPrincipal principal
  ) {
    String txnType = requireTransactionType(request.txnType());
    BigDecimal quantity = requirePositive("quantity", request.quantity());
    InventoryBatchRecord batch = inventoryRepository.lockBatchById(request.inventoryBatchId())
        .orElseThrow(() -> notFound("inventory_batch", request.inventoryBatchId()));
    BigDecimal nextQuantity = nextQuantity(batch, txnType, quantity);
    String nextStatus = statusAfterTransaction(batch, txnType, nextQuantity);
    InventoryTransactionRecord transaction = new InventoryTransactionRecord(
        UUID.randomUUID(),
        batch.id(),
        txnType,
        quantity,
        principal.userId(),
        Instant.now(),
        emptyToNull(request.reason())
    );
    InventoryTransactionRecord saved = inventoryRepository.saveTransaction(transaction, principal.userId());
    inventoryRepository.updateBatchQuantityAndStatus(batch.id(), nextQuantity, nextStatus, principal.userId());
    return InventoryTransactionResponse.from(saved);
  }

  private BigDecimal nextQuantity(InventoryBatchRecord batch, String txnType, BigDecimal quantity) {
    if (INCREASE_TYPES.contains(txnType)) {
      return batch.quantity().add(quantity);
    }
    if (DECREASE_TYPES.contains(txnType)) {
      BigDecimal next = batch.quantity().subtract(quantity);
      if (next.compareTo(BigDecimal.ZERO) < 0) {
        throw new BusinessException(
            "INVENTORY_QUANTITY_INSUFFICIENT",
            "库存数量不足",
            HttpStatus.CONFLICT,
            Map.of(
                "inventoryBatchId", batch.id(),
                "availableQuantity", batch.quantity(),
                "requestedQuantity", quantity
            )
        );
      }
      return next;
    }
    return quantity;
  }

  private String statusAfterTransaction(InventoryBatchRecord batch, String txnType, BigDecimal nextQuantity) {
    if ("SCRAP".equals(txnType) && nextQuantity.compareTo(BigDecimal.ZERO) <= 0) {
      return "SCRAPPED";
    }
    return statusFor(nextQuantity, batch.expiryDate(), batch.openedAt());
  }

  private String statusFor(BigDecimal quantity, LocalDate expiryDate, LocalDate openedAt) {
    if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
      return "DEPLETED";
    }
    if (expiryDate != null && expiryDate.isBefore(LocalDate.now())) {
      return "EXPIRED";
    }
    return openedAt == null ? "IN_STOCK" : "OPENED";
  }

  private InventoryBatchRecord batch(UUID id) {
    return inventoryRepository.findBatchById(id)
        .orElseThrow(() -> notFound("inventory_batch", id));
  }

  private BusinessException notFound(String entityType, UUID entityId) {
    return new BusinessException(
        "NOT_FOUND",
        "资源不存在",
        HttpStatus.NOT_FOUND,
        Map.of("entityType", entityType, "entityId", entityId)
    );
  }

  private String requireTransactionType(String value) {
    String type = requireText("txnType", value).toUpperCase(Locale.ROOT);
    if (!TRANSACTION_TYPES.contains(type)) {
      throw new BusinessException(
          "INVENTORY_TXN_TYPE_INVALID",
          "库存流水类型不合法",
          HttpStatus.BAD_REQUEST,
          Map.of("txnType", String.valueOf(value))
      );
    }
    return type;
  }

  private BigDecimal requireNonNegative(String field, BigDecimal value) {
    if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
      throw new BusinessException(
          "VALIDATION_FAILED",
          "请求参数校验失败",
          HttpStatus.BAD_REQUEST,
          Map.of(field, "must be >= 0")
      );
    }
    return value;
  }

  private BigDecimal requirePositive(String field, BigDecimal value) {
    if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
      throw new BusinessException(
          "VALIDATION_FAILED",
          "请求参数校验失败",
          HttpStatus.BAD_REQUEST,
          Map.of(field, "must be > 0")
      );
    }
    return value;
  }

  private String requireText(String field, String value) {
    String normalized = emptyToNull(value);
    if (normalized == null) {
      throw new BusinessException(
          "VALIDATION_FAILED",
          "请求参数校验失败",
          HttpStatus.BAD_REQUEST,
          Map.of(field, "required")
      );
    }
    return normalized;
  }

  private String emptyToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
