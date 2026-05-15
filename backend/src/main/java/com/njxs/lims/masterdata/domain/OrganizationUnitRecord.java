package com.njxs.lims.masterdata.domain;

import java.util.UUID;

public record OrganizationUnitRecord(
    UUID id,
    UUID parentId,
    String name,
    String status
) {
}
