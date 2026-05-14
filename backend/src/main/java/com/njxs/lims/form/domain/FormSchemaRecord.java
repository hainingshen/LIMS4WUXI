package com.njxs.lims.form.domain;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.UUID;

public record FormSchemaRecord(
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
}
