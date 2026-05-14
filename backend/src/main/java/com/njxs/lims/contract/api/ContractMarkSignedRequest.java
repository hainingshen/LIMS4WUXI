package com.njxs.lims.contract.api;

import java.time.Instant;
import java.util.UUID;

public record ContractMarkSignedRequest(
    Instant signedAt,
    UUID signedFileId
) {
}
