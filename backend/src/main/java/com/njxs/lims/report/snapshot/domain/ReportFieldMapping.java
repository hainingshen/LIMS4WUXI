package com.njxs.lims.report.snapshot.domain;

public record ReportFieldMapping(
    String targetField,
    String reportLabel,
    String sourceEntity,
    String sourceFieldOrRule,
    boolean required,
    boolean autoFill,
    String notes,
    int sortOrder
) {
}
