package com.njxs.lims.change.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record ChangeRequestCreateRequest(
    @NotBlank String targetType,
    @NotNull UUID targetId,
    @NotBlank String changeType,
    @NotBlank String reason,
    UUID attachmentFileId,
    @Valid @NotEmpty List<ChangeRequestItemRequest> items
) {
}
