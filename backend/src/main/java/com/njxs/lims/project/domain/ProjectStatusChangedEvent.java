package com.njxs.lims.project.domain;

import java.util.UUID;

public record ProjectStatusChangedEvent(
    UUID projectId,
    String sourceStatus,
    String targetStatus,
    String reason
) {
}
