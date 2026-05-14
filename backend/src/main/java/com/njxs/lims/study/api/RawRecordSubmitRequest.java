package com.njxs.lims.study.api;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record RawRecordSubmitRequest(
    @NotBlank String templateCode,
    @NotNull JsonNode recordData,
    List<UUID> attachmentFileIds
) {
}
