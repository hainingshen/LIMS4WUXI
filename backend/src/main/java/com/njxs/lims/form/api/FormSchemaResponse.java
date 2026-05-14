package com.njxs.lims.form.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.njxs.lims.form.domain.FormSchemaRecord;
import java.time.Instant;
import java.util.UUID;

public record FormSchemaResponse(
    UUID id,
    String formCode,
    int versionNo,
    JsonNode schemaJson,
    JsonNode uiSchemaJson,
    String status,
    Instant publishedAt,
    UUID publishedBy,
    String note
) {

  public static FormSchemaResponse from(FormSchemaRecord record) {
    return new FormSchemaResponse(
        record.id(),
        record.formCode(),
        record.versionNo(),
        record.schemaJson(),
        record.uiSchemaJson(),
        record.status(),
        record.publishedAt(),
        record.publishedBy(),
        record.note()
    );
  }
}
