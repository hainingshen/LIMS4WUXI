package com.njxs.lims.masterdata.persistence;

import com.njxs.lims.masterdata.domain.DictionaryItemRecord;
import java.util.List;

public interface DictionaryItemRepository {

  List<DictionaryItemRecord> find(String dictCode);
}
