package com.njxs.lims.auth.store;

import com.njxs.lims.auth.domain.ClientType;
import java.time.Instant;
import java.util.UUID;

public record RefreshTokenSession(
    UUID userId,
    ClientType clientType,
    Instant expiresAt
) {
}
