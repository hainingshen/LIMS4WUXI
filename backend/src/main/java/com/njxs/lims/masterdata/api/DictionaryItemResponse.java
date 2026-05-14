package com.njxs.lims.masterdata.api;

import com.njxs.lims.masterdata.domain.DictionaryItemRecord;

public record DictionaryItemResponse(
    String dictCode,
    String itemCode,
    String labelZh,
    String labelEn,
    int sortOrder,
    boolean enabled
) {

  public static DictionaryItemResponse from(DictionaryItemRecord record) {
    return new DictionaryItemResponse(
        record.dictCode(),
        record.itemCode(),
        record.labelZh(),
        record.labelEn(),
        record.sortOrder(),
        record.enabled()
    );
  }
}
