package com.njxs.lims.inventory.api;

import com.njxs.lims.audit.Audited;
import com.njxs.lims.auth.domain.ClientType;
import com.njxs.lims.auth.service.AuthContext;
import com.njxs.lims.common.api.PageResponse;
import com.njxs.lims.inventory.service.InventoryService;
import com.njxs.lims.permission.RequiresAction;
import com.njxs.lims.permission.RequiresClient;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiresClient(ClientType.INTERNAL)
@ConditionalOnProperty(prefix = "lims.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class InventoryController {

  private final InventoryService inventoryService;

  public InventoryController(InventoryService inventoryService) {
    this.inventoryService = inventoryService;
  }

  @GetMapping("/inventory-batches")
  @RequiresAction("INVENTORY_READ")
  public PageResponse<InventoryBatchResponse> listBatches(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int pageSize,
      @RequestParam(required = false) String keyword
  ) {
    return inventoryService.listBatches(keyword, page, pageSize);
  }

  @PostMapping("/inventory-batches")
  @Audited(actionCode = "INVENTORY_TXN_CREATE", entityType = "inventory_batch")
  @RequiresAction("INVENTORY_TXN_CREATE")
  public InventoryBatchResponse upsertBatch(@Valid @RequestBody InventoryBatchUpsertRequest request) {
    return inventoryService.upsertBatch(request, AuthContext.require());
  }

  @GetMapping("/inventory-transactions")
  @RequiresAction("INVENTORY_READ")
  public PageResponse<InventoryTransactionResponse> listTransactions(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int pageSize,
      @RequestParam(required = false) UUID inventoryBatchId
  ) {
    return inventoryService.listTransactions(inventoryBatchId, page, pageSize);
  }

  @PostMapping("/inventory-transactions")
  @ResponseStatus(HttpStatus.CREATED)
  @Audited(actionCode = "INVENTORY_TXN_CREATE", entityType = "inventory_transaction")
  @RequiresAction("INVENTORY_TXN_CREATE")
  public InventoryTransactionResponse createTransaction(
      @Valid @RequestBody InventoryTransactionCreateRequest request
  ) {
    return inventoryService.createTransaction(request, AuthContext.require());
  }
}
