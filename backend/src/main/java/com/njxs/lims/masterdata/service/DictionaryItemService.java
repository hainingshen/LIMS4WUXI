package com.njxs.lims.masterdata.service;

import com.njxs.lims.masterdata.domain.DictionaryItemRecord;
import com.njxs.lims.masterdata.persistence.DictionaryItemRepository;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "lims.persistence", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DictionaryItemService {

  private final DictionaryItemRepository dictionaryItemRepository;

  public DictionaryItemService(DictionaryItemRepository dictionaryItemRepository) {
    this.dictionaryItemRepository = dictionaryItemRepository;
  }

  public List<DictionaryItemRecord> list(String dictCode) {
    return dictionaryItemRepository.find(dictCode);
  }
}
