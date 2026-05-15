package com.njxs.lims.inventory.persistence;

import com.njxs.lims.inventory.domain.InventoryBatchRecord;
import com.njxs.lims.inventory.domain.InventoryTransactionRecord;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryRepository {

  List<InventoryBatchRecord> listBatches(String keyword, int limit, int offset);

  int countBatches(String keyword);

  Optional<InventoryBatchRecord> findBatchById(UUID id);

  Optional<InventoryBatchRecord> lockBatchById(UUID id);

  InventoryBatchRecord insertBatch(InventoryBatchRecord record, UUID actorUserId);

  Optional<InventoryBatchRecord> updateBatch(InventoryBatchRecord record, UUID actorUserId);

  InventoryBatchRecord updateBatchQuantityAndStatus(UUID id, BigDecimal quantity, String status, UUID actorUserId);

  List<InventoryTransactionRecord> listTransactions(UUID inventoryBatchId, int limit, int offset);

  int countTransactions(UUID inventoryBatchId);

  InventoryTransactionRecord saveTransaction(InventoryTransactionRecord record, UUID actorUserId);
}
