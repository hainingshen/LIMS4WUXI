package com.njxs.lims.sample.domain;

import java.time.Instant;
import java.util.UUID;

public record SamplePhoto(
    UUID id,
    UUID sampleId,
    UUID fileId,
    String photoType,
    String note,
    Instant uploadedAt,
    UUID uploadedBy,
    int sortOrder
) {
}
