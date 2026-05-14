package com.njxs.lims.masterdata.domain;

public record DictionaryItemRecord(
    String dictCode,
    String itemCode,
    String labelZh,
    String labelEn,
    int sortOrder,
    boolean enabled
) {
}
