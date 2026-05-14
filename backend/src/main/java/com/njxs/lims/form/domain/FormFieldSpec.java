package com.njxs.lims.form.domain;

public record FormFieldSpec(
    String fieldKey,
    String labelZh,
    String labelEn,
    String type,
    boolean required,
    String dictionary,
    String entity,
    String lockPolicy
) {
}
